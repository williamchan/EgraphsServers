package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import org.joda.money.Money
import services.{Payment, Utils, Time}
import models.CashTransaction.EgraphPurchase
import services.AppConfig
import db.{FilterThreeTables, KeyedCaseClass, Schema, Saves}
import com.google.inject.{Provider, Inject}
import services.Finance.TypeConversions._

case class OrderServices @Inject() (
  store: OrderStore,
  customerStore: CustomerStore,
  productStore: ProductStore,
  payment: Payment,
  cashTransactionServices: Provider[CashTransactionServices],
  egraphServices: Provider[EgraphServices])

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
  updated: Timestamp = Time.defaultTimestamp,
  services: OrderServices = AppConfig.instance[OrderServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  import Order._

  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): Order = {
    services.store.save(this)
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
    services.customerStore.get(buyerId)
  }

  /** Retrieves the receiving Customer from the database */
  def recipient: Customer = {
    services.customerStore.get(recipientId)
  }

  /** Retrieves the purchased Product from the database */
  def product: Product = {
    services.productStore.get(productId)
  }

  /** Returns an order configured with the provided payment state */
  def withPaymentState(paymentState: PaymentState) = {
    copy(paymentStateString = paymentState.stateValue)
  }

  /** Produces an OrderCharge whose use will charge the `stripeCardTokenId` for the order */
  def charge: OrderCharge = {
    require(stripeCardTokenId != None, "Can not charge an order without a valid stripe card token")

    val cashTransaction = CashTransaction(accountId=buyerId, services=services.cashTransactionServices.get)
      .withCash(amountPaid)
      .withType(EgraphPurchase)

    OrderCharge(
      this.withPaymentState(Order.PaymentState.Charged),
      stripeCardTokenId.get,
      cashTransaction,
      services.payment
    )
  }

  /**
   * Renders the Order as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val customerStore = services.customerStore
    val buyer = customerStore.findById(buyerId).get
    val recipient = if (buyerId != recipientId) customerStore.findById(recipientId).get else buyer

    val requiredFields = Map(
      "id" -> id,
      "product" -> product.renderedForApi,
      "buyerId" -> buyer.id,
      "buyerName" -> buyer.name,
      "recipientId" -> recipient.id,
      "recipientName" -> recipientName,
      "amountPaidInCents" -> amountPaid.getAmountMinor
    )

    val optionalFields = Utils.makeOptionalFieldMap(
      List(
        "requestedMessage" -> requestedMessage,
        "messageToCelebrity" -> messageToCelebrity
      )
    )

    requiredFields ++ optionalFields ++ renderCreatedUpdatedForApi
  }

  /**
   * Produces a new Egraph associated with this order.
   */
  def newEgraph: Egraph = {
    Egraph(orderId=id, services=services.egraphServices.get).withState(AwaitingVerification)
  }
  
  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Order.unapply(this)
}

case class FulfilledOrder(order: Order, egraph: Egraph)

class OrderStore @Inject() (schema: db.Schema) extends Saves[Order] with SavesCreatedUpdated[Order] {
  //
  // Public methods
  //
  /**
   * Returns a completed Order/Egraph combination with the given ID
   */
  def findFulfilledWithId(id: Long): Option[FulfilledOrder] = {
    import schema.{orders, egraphs}
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

  def FindByCelebrity(celebrityId: Long, filters: FilterThreeTables[Celebrity, Product, Order]*):Iterable[Order] = {
    import schema.{celebrities, products, orders}

    from(celebrities, products, orders)((celebrity, product, order) =>
      where(
        celebrity.id === celebrityId and
          celebrity.id === product.celebrityId and
          product.id === order.productId and
          FilterThreeTables.reduceFilters(filters, celebrity, product, order)
      )
        select(order)
        orderBy(order.created asc)
    )
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
    object ActionableOnly extends FilterThreeTables[Celebrity, Product, Order] {
      override def test(celebrity: Celebrity, product: Product, order: Order) = {
        notExists(
          from(schema.egraphs)(egraph =>
            where((egraph.orderId === order.id) and (egraph.stateValue in Seq(Verified.value, AwaitingVerification.value)))
              select(egraph.id)
          )
        )
      }
    }

    /**
     * Returns only orders with the given orderId when composed with FindByCelebrity
     */
    case class OrderId(id: Long) extends FilterThreeTables[Celebrity, Product, Order] {
      override def test(celebrity: Celebrity, product: Product, order: Order) = {
        (order.id === id)
      }
    }
  }

  //
  // Saves[Order] methods
  //
  override val table = schema.orders

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

object OrderStore {
  object FindByCelebrity {
    /**
     * Returns only orders that are actionable by the celebrity that owns them when
     * composed with FindByCelebrity.
     *
     * In model terms, these are these are any Orders that don't have an Egraph that
     * is either Verified or AwaitingVerification.
     */
    class ActionableOnly @Inject() (schema: Schema) extends FilterThreeTables[Celebrity, Product, Order] {
      override def test(celebrity: Celebrity, product: Product, order: Order) = {
        notExists(
          from(schema.egraphs)(egraph =>
            where((egraph.orderId === order.id) and (egraph.stateValue in Seq(Verified.value, AwaitingVerification.value)))
              select(egraph.id)
          )
        )
      }
    }

    /**
     * Returns only orders with the given orderId when composed with FindByCelebrity
     */
    case class OrderId(id: Long) extends FilterThreeTables[Celebrity, Product, Order] {
      override def test(celebrity: Celebrity, product: Product, order: Order) = {
        (order.id === id)
      }
    }
  }

}

// TOOD(erem) remove all store functionality from the Order object
object Order {
  //
  // Public Methods
  //

  /** Encapsulates the act of charging for an order. */
  case class OrderCharge private[Order] (order: Order,
                                         stripeCardTokenId: String,
                                         transaction: CashTransaction,
                                         payment: Payment) {
    def issueAndSave(): OrderCharge = {
      val stripeCharge = payment.charge(
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
}
