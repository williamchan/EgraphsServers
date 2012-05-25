package models

import java.sql.Timestamp
import org.joda.money.Money
import services.db.{FilterOneTable, KeyedCaseClass, Schema, Saves}
import services.Finance.TypeConversions._
import services._
import com.google.inject._
import mail.Mail
import payment.{Charge, Payment}
import play.mvc.Router.ActionDefinition
import org.squeryl.Query
import models.Egraph.EgraphState._
import models.CashTransaction.{PurchaseRefund, EgraphPurchase}
import org.apache.commons.mail.{Email, HtmlEmail}
import scala.util.Random

case class OrderServices @Inject() (
  store: OrderStore,
  customerStore: CustomerStore,
  celebrityStore: CelebrityStore,
  productStore: ProductStore,
  payment: Payment,
  mail: Mail,
  cashTransactionServices: Provider[CashTransactionServices],
  egraphServices: Provider[EgraphServices],
  audioPromptServices: Provider[OrderAudioPromptServices]
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
    require(!recipientName.isEmpty, "Order: recipientName must be specified")
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

  /** Call this to associate a Stripe Charge with this Order */
  def withChargeInfo(stripeCardTokenId: String, stripeCharge: Charge): Order = {
    require(id != 0, "Order must have an id")
    CashTransaction(accountId = buyer.account.id, orderId = Some(id), services = services.cashTransactionServices.get)
      .withCash(amountPaid)
      .withType(EgraphPurchase)
      .save()

    this.copy(stripeCardTokenId = Some(stripeCardTokenId), stripeChargeId = Some(stripeCharge.id))
      .withPaymentState(PaymentState.Charged)
  }

  /**
   * refund functionality is ready to be called, but is not exposed anywhere.
   * Hopefully we will not have many refund requests when we launch.
   */
  def refund(): (Order, Charge) = {
    require(paymentState == PaymentState.Charged, "Refunding an Order requires that the Order be already Charged")
    require(stripeChargeId.isDefined, "Refunding an Order requires that the Order be already Charged")

    val refundedCharge = services.payment.refund(stripeChargeId.get)

    CashTransaction(accountId = buyerId, orderId = Some(id), services = services.cashTransactionServices.get)
      .withCash(amountPaid.negated())
      .withType(PurchaseRefund)
      .save()

    val refundedOrder = withPaymentState(PaymentState.Refunded)
    (refundedOrder, refundedCharge)
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
    val email = prepareEgraphsSignedEmail()
    services.mail.send(email)
  }

  protected[models] def prepareEgraphsSignedEmail(): Email = {
    val celebrity = services.celebrityStore.findByOrderId(id).get
    val email = new HtmlEmail()
    val linkActionDefinition: ActionDefinition = Utils.lookupUrl("WebsiteControllers.getEgraph", Map("orderId" -> id.toString))
    linkActionDefinition.absolute()

    val buyingCustomer = this.buyer
    val receivingCustomer = this.recipient
    println("celebrity.urlSlug.get " + celebrity.urlSlug.get)
    email.setFrom(celebrity.urlSlug.get + "@egraphs.com", celebrity.publicName.get)
    email.addTo(receivingCustomer.account.email, recipientName)
    if (buyingCustomer != receivingCustomer) {
      email.addCc(buyingCustomer.account.email, buyingCustomer.name)
    }

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
    email
  }
  /**
   * Generates an audio prompt based on an random selection from audioPromptTemplates.
   */
  protected[models] def generateAudioPrompt(indexOfAudioPromptTemplate: Option[Int] = None): String = {
    val i = indexOfAudioPromptTemplate.getOrElse(Order.random.nextInt(Order.audioPromptTemplates.length))
    val orderAudioPrompt = OrderAudioPrompt(
      audioPromptTemplate=Order.audioPromptTemplates(i),
      celebName=product.celebrity.publicName.get,
      recipientName=recipientName,
      services=services.audioPromptServices.get()
    )
    orderAudioPrompt.audioPrompt
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
      "reviewStatus" -> reviewStatus,
      "audioPrompt" -> generateAudioPrompt()
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

  lazy val random = new Random

  // Update this list with marketing-approved copy per SER-56
  lazy val audioPromptTemplates = List(
    "From {signer_name} to {recipient_name} with love",
    "Hi {recipient_name}, this is {signer_name}. Let’s grow old together, that might be fun",
    "Roses are red, violets are blue, this is an Egraph from {signer_name} to {recipient_name}"
  )

  //
  // Public Methods
  //

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

case class OrderAudioPromptServices @Inject() (templateEngine: TemplateEngine)

object OrderAudioPromptField extends Utils.Enum {
  sealed trait EnumVal extends Value

  /** Public name of the celebrity */
  val CelebrityName = new EnumVal { val name = "signer_name" }

  /** Name of the person receiving the egraph */
  val RecipientName = new EnumVal { val name = "recipient_name"}
}

/**
 * @param audioPromptTemplate title template as specified on the [[models.Product]]
 * @param celebName the celebrity's public name
 * @param recipientName name of the [[models.Customer]] receiving the order.
 * @param services Services needed for the OrderAudioPrompt to manipulate its data properly.
 */
case class OrderAudioPrompt(private val audioPromptTemplate: String,
                            private val celebName: String,
                            private val recipientName: String,
                            private val services: OrderAudioPromptServices = AppConfig.instance[OrderAudioPromptServices]) {
  //
  // Public methods
  //
  def audioPrompt: String = {
    services.templateEngine.evaluate(audioPromptTemplate, templateParams)
  }

  //
  // Private methods
  //
  private val templateParams: Map[String, String] = {
    import OrderAudioPromptField._
    val pairs = for (templateField <- OrderAudioPromptField.values) yield {
      val paramValue = templateField match {
        case CelebrityName => celebName
        case RecipientName => recipientName
      }
      (templateField.name, paramValue)
    }
    pairs.toMap
  }
}
