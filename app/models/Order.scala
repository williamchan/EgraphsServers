package models

import java.sql.Timestamp
import org.joda.money.Money
import models.CashTransaction.EgraphPurchase
import services.db.{FilterOneTable, KeyedCaseClass, Schema, Saves}
import services.Finance.TypeConversions._
import org.apache.commons.mail.HtmlEmail
import services._
import com.google.inject._
import mail.Mail
import payment.Payment
import play.mvc.Router.ActionDefinition
import org.squeryl.Query
import models.Egraph.EgraphState._

case class OrderServices @Inject() (
  store: OrderStore,
  customerStore: CustomerStore,
  celebrityStore: CelebrityStore,
  productStore: ProductStore,
  payment: Payment,
  mail: Mail,
  cashTransactionServices: Provider[CashTransactionServices],
  egraphServices: Provider[EgraphServices]
)

/**
 * Persistent entity representing the Orders made upon Products of our service
 */
case class Order(
  id: Long = 0,
  productId: Long = 0,
  inventoryBatchId: Long = 0,
  buyerId: Long = 0,
  recipientId: Long = 0,
  recipientName: String = "",
  paymentStateString: String = Order.PaymentState.NotCharged.stateValue,
  reviewStatus: String = Order.ReviewStatus.PendingAdminReview.stateValue,
  rejectionReason: Option[String] = None,
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

  def approveByAdmin(admin: Administrator): Order = {
    require(admin != null, "Must be approved by an Administrator")
    require(reviewStatus == Order.ReviewStatus.PendingAdminReview.stateValue, "Must be PendingAdminReview before approving by admin")
    this.copy(reviewStatus = Order.ReviewStatus.ApprovedByAdmin.stateValue)
  }

  def rejectByAdmin(admin: Administrator, rejectionReason: Option[String] = None): Order = {
    require(admin != null, "Must be rejected by an Administrator")
    require(reviewStatus == Order.ReviewStatus.PendingAdminReview.stateValue, "Must be PendingAdminReview before rejecting by admin")
    val order = this.copy(reviewStatus = Order.ReviewStatus.RejectedByAdmin.stateValue, rejectionReason = rejectionReason)
    order
  }

  def rejectByCelebrity(celebrity: Celebrity, rejectionReason: Option[String] = None): Order = {
    require(celebrity != null, "Must be rejected by Celebrity associated with this Order")
    require(celebrity.id == product.celebrityId, "Must be rejected by Celebrity associated with this Order")
    require(reviewStatus == Order.ReviewStatus.ApprovedByAdmin.stateValue, "Must be ApprovedByAdmin before rejecting by celebrity")
    val order = this.copy(reviewStatus = Order.ReviewStatus.RejectedByCelebrity.stateValue, rejectionReason = rejectionReason)
    order
  }

  def sendEgraphSignedMail() {
    val celebrity = services.celebrityStore.findByOrderId(id).get
    val email = new HtmlEmail()
    val linkActionDefinition: ActionDefinition = Utils.lookupUrl("WebsiteControllers.getEgraph", Map("orderId" -> id.toString))
    linkActionDefinition.absolute()

    email.setFrom(celebrity.urlSlug.get + "@egraphs.com", celebrity.publicName.get)
    email.addTo(recipient.account.email, recipientName)
    email.addReplyTo("noreply@egraphs.com")
    email.setSubject("I just finished signing your Egraph")
    email.setMsg(
      views.Application.email.html.egraph_signed_email(
        celebrity,
        product,
        this,
        linkActionDefinition.url
      ).toString().trim()
    )

    services.mail.send(email)
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
      "amountPaidInCents" -> amountPaid.getAmountMinor,
      "reviewStatus" -> reviewStatus
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
      val charge = payment.charge(
        order.amountPaid,
        stripeCardTokenId,
        "Egraph Order=" + order.id
      )

      val savedTransaction = transaction.save()
      val savedOrder = order.copy(
        transactionId=Some(savedTransaction.id),
        stripeChargeId=Some(charge.id)
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

  /** Specifies the Order's current status relative to content auditing */
  sealed abstract class ReviewStatus(val stateValue: String)

  object ReviewStatus {

    case object PendingAdminReview extends ReviewStatus("PendingAdminReview")

    /** Order is signerActionable */
    case object ApprovedByAdmin extends ReviewStatus("ApprovedByAdmin")

    case object RejectedByAdmin extends ReviewStatus("RejectedByAdmin")

    case object RejectedByCelebrity extends ReviewStatus("RejectedByCelebrity")

    val all = Utils.toMap[String, ReviewStatus](
      Seq(PendingAdminReview, ApprovedByAdmin, RejectedByAdmin, RejectedByCelebrity), key = (state) => state.stateValue
    )
  }

}

class OrderStore @Inject() (schema: Schema) extends Saves[Order] with SavesCreatedUpdated[Order] {
  import org.squeryl.PrimitiveTypeMode._
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
          egraph.stateValue === Published.value
      )
        select (FulfilledOrder(order, egraph))
    ).headOption
  }

  /**
   * Finds a list of Orders based on the id of the Celebrity
   * who owns the Product that was purchased.
   */
  def findByCelebrity(celebrityId: Long, filters: FilterOneTable[Order]*): Query[Order] = {
    import schema.{celebrities, products, orders}

    from(celebrities, products, orders)((celebrity, product, order) =>
      where(
        celebrity.id === celebrityId and
          celebrity.id === product.celebrityId and
          product.id === order.productId and
          FilterOneTable.reduceFilters(filters, order)
      )
        select (order)
        orderBy (order.created asc)
    )
  }

  def findByFilter(filters: FilterOneTable[Order]*): Query[Order] = {
    import schema.orders

    from(orders)((order) =>
      where(
        FilterOneTable.reduceFilters(filters, order)
      )
        select (order)
        orderBy (order.created asc)
    )
  }

  def countOrders(inventoryBatchIds: Seq[Long]): Int = {
    from(schema.orders)(order =>
      where(order.inventoryBatchId in inventoryBatchIds)
        compute (count)
    ).toInt
  }

  //
  // Saves[Order] methods
  //
  override val table = schema.orders

  override def defineUpdate(theOld: Order, theNew: Order) = {
    updateIs(
      theOld.productId := theNew.productId,
      theOld.inventoryBatchId := theNew.inventoryBatchId,
      theOld.buyerId := theNew.buyerId,
      theOld.transactionId := theNew.transactionId,
      theOld.paymentStateString := theNew.paymentStateString,
      theOld.reviewStatus := theNew.reviewStatus,
      theOld.rejectionReason := theNew.rejectionReason,
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

class OrderQueryFilters @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  def actionableOnly = List(actionableEgraphs, approvedByAdmin)

  private def actionableEgraphs: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        val nonActionableStates = Seq(Published.value, ApprovedByAdmin.value,
          AwaitingVerification.value, PassedBiometrics.value, FailedBiometrics.value)

        notExists(
          from(schema.egraphs)(egraph =>
            where((egraph.orderId === order.id) and
              (egraph.stateValue in nonActionableStates))
              select(egraph.id)
          )
        )
      }
    }
  }

  private def approvedByAdmin: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order.reviewStatus === Order.ReviewStatus.ApprovedByAdmin.stateValue)
      }
    }
  }

  def pendingAdminReview: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order.reviewStatus === Order.ReviewStatus.PendingAdminReview.stateValue)
      }
    }
  }

  def rejected: FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order.reviewStatus in Seq(Order.ReviewStatus.RejectedByAdmin.stateValue, Order.ReviewStatus.RejectedByCelebrity.stateValue))
      }
    }
  }

  def orderId(id: Long): FilterOneTable[Order] = {
    new FilterOneTable[Order] {
      override def test(order: Order) = {
        (order.id === id)
      }
    }
  }
}

