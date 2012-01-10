package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import org.squeryl.dsl.ast.LogicalBoolean
import org.joda.money.Money
import libs.Finance.TypeConversions._
import libs.{Payment, Utils, Serialization, Time}
import models.CashTransaction.EgraphPurchase

/**
 * Persistent entity representing the Orders made upon Products of our service
 */
case class Order(
  id: Long = 0,
  productId: Long = 0,
  buyerId: Long = 0,
  recipientId: Long = 0,
  recipientName: String = "",
  paymentStateString: String = Order.PaymentState.NotCharged.stateValue,
  transactionId: Option[Long] = None,
  stripeCardTokenId: Option[String] = None,
  stripeChargeId: Option[String] = None,
  amountPaidInCurrency: BigDecimal = 0,
  messageToCelebrity: Option[String] = None,
  requestedMessage: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import Order._

  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): Order = {
    Order.save(this)
  }

  def amountPaid: Money = {
    amountPaidInCurrency.toMoney()
  }

  /** Returns the current payment state */
  def paymentState: PaymentState = {
    PaymentState.all(paymentStateString)
  }

  /** Retrieves the purchasing Customer from the database. */
  def buyer: Customer = {
    Customer.get(buyerId)
  }

  /** Retrieves the receiving Customer from the database */
  def recipient: Customer = {
    Customer.get(recipientId)
  }

  /** Retrieves the purchased Product from the database */
  def product: Product = {
    Product.get(productId)
  }

  /** Returns an order configured with the provided payment state */
  def withPaymentState(paymentState: PaymentState) = {
    copy(paymentStateString = paymentState.stateValue)
  }

  /** Produces an OrderCharge whose use will charge the `stripeCardTokenId` for the order */
  def charge: OrderCharge = {
    require(stripeCardTokenId != None, "Can not charge an order without a valid stripe card token")

    val cashTransaction = CashTransaction(accountId=buyerId)
      .withCash(amountPaid)
      .withType(EgraphPurchase)

    OrderCharge(
      this.withPaymentState(Order.PaymentState.Charged), stripeCardTokenId.get, cashTransaction
    )
  }

  /**
   * Renders the Order as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val buyer = Customer.findById(buyerId).get
    val recipient = if (buyerId != recipientId) Customer.findById(recipientId).get else buyer

    val requiredFields = Map(
      "id" -> id,
      "product" -> product.renderedForApi,
      "buyerId" -> buyer.id,
      "buyerName" -> buyer.name,
      "recipientId" -> recipient.id,
      "recipientName" -> recipientName,
      "amountPaidInCents" -> amountPaid.getAmountMinor
    )

    val optionalFields = Serialization.makeOptionalFieldMap(
      List(
        ("requestedMessage" -> requestedMessage),
        ("messageToCelebrity" -> messageToCelebrity)
      )
    )

    requiredFields ++ optionalFields ++ renderCreatedUpdatedForApi
  }

  /**
   * Produces a new Egraph associated with this order.
   */
  def newEgraph: Egraph = {
    Egraph(orderId=id).withState(AwaitingVerification)
  }
  
  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Order.unapply(this)
}

case class FulfilledOrder(order: Order, egraph: Egraph)

object Order extends Saves[Order] with SavesCreatedUpdated[Order] {
  //
  // Public Methods
  //
  /**
   * Returns a completed Order/Egraph combination with the given ID
   */
  def findFulfilledWithId(id: Long): Option[FulfilledOrder] = {
    import Schema.{orders, egraphs}
    from(orders, egraphs)((order, egraph) =>
      where(
        order.id === id and
        egraph.orderId === order.id and
        egraph.stateValue === Verified.value
      )
      select (FulfilledOrder(order, egraph))
    ).headOption
  }

  /**
   * Callable object that finds a list of Orders based on the id of the Celebrity
   * who owns the Product that was purchased.
   */
  object FindByCelebrity {
    def apply(celebrityId: Long, filters: Filter*):Iterable[Order] = {
      import Schema.{celebrities, products, orders}

      from(celebrities, products, orders)((celebrity, product, order) =>
        where(
          celebrity.id === celebrityId and
          celebrity.id === product.celebrityId and
          product.id === order.productId and
          reduceFilters(filters, celebrity, product, order)
        )
        select(order)
        orderBy(order.created asc)
      )
    }

    /**
     * Base definition of an object that can apply a Squeryl filter against the join
     * between Schema.celebrities, Schema.products, Schema.orders that occurs in the
     * Order.FindByCelebrity function.
     */
    sealed trait Filter {
      /**
       * Returns the logical filter to apply upon the join between Celebrity/Product/Order
       */
      def test(celebrity: Celebrity, product: Product, order: Order): LogicalBoolean
    }

    /**
     * Set of Filters you can compose with your query to FindByCelebrity
     */
    object Filters {

      /**
       * Returns only orders that are actionable by the celebrity that owns them when
       * composed with FindByCelebrity.
       * 
       * In model terms, these are these are any Orders that don't have an Egraph that
       * is either Verified or AwaitingVerification.
       */
      object ActionableOnly extends Filter {
        override def test(celebrity: Celebrity, product: Product, order: Order) = {
          notExists(
            from(Schema.egraphs)(egraph =>
              where((egraph.orderId === order.id) and (egraph.stateValue in Seq(Verified.value, AwaitingVerification.value)))
              select(egraph.id)
            )
          )
        }
      }

      /**
       * Returns only orders with the given orderId when composed with FindByCelebrity
       */
      case class OrderId(id: Long) extends Filter {
        override def test(celebrity: Celebrity, product: Product, order: Order) = {
          (order.id === id)
        }
      }
    }

    //
    // Private methods (FindByCelebrity)
    //
    private def reduceFilters(
      filters: Iterable[Filter],
      celebrity: Celebrity,
      product: Product,
      order: Order): LogicalBoolean =
    {
      filters.headOption match {
        // Just return the trivial comparison if there were no filters
        case None =>
          (1 === 1)

        // Otherwise reduce the collection of filters against the first
        case Some(firstFilter) =>
          filters.tail.foldLeft(firstFilter.test(celebrity, product, order))(
            (compositeFilter, nextFilter) =>
              (nextFilter.test(celebrity, product, order) and compositeFilter)
          )
      }
    }
  }

  /** Encapsulates the act of charging for an order. */
  case class OrderCharge private[Order] (order: Order,
                                         stripeCardTokenId: String,
                                         transaction: CashTransaction) {
    def issueAndSave(): OrderCharge = {
      val stripeCharge = Payment.charge(
        order.amountPaid,
        stripeCardTokenId,
        "Egraph Order=" + order.id
      )

      val savedTransaction = transaction.save()
      val savedOrder = order.copy(
        transactionId=Some(savedTransaction.id),
        stripeChargeId=Some(stripeCharge.getId)
      ).save()

      this.copy(order=savedOrder, transaction=savedTransaction)
    }
  }

  /** Specifies the Order's current status relative to payment */
  sealed abstract class PaymentState(val stateValue: String)

  object PaymentState {
    /** We have not yet charged for the order */
    case object NotCharged extends PaymentState("NotCharged")

    /** We have successfully charged the order */
    case object Charged extends PaymentState("Charged")

    /** We have refunded the order */
    case object Refunded extends PaymentState("Refunded")

    val all = Utils.toMap[String, PaymentState](
      Seq(NotCharged, Charged, Refunded), key=(state) => state.stateValue
    )
  }

  //
  // Saves[Order] methods
  //
  override val table = Schema.orders

  override def defineUpdate(theOld: Order, theNew: Order) = {
    updateIs(
      theOld.productId := theNew.productId,
      theOld.buyerId := theNew.buyerId,
      theOld.transactionId := theNew.transactionId,
      theOld.paymentStateString := theNew.paymentStateString,
      theOld.stripeCardTokenId := theNew.stripeCardTokenId,
      theOld.stripeChargeId := theNew.stripeChargeId,
      theOld.amountPaidInCurrency := theNew.amountPaidInCurrency,
      theOld.recipientId := theNew.recipientId,
      theOld.recipientName := theNew.recipientName,
      theOld.messageToCelebrity := theNew.messageToCelebrity,
      theOld.requestedMessage := theNew.requestedMessage,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Order] methods
  //
  override def withCreatedUpdated(toUpdate: Order, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
