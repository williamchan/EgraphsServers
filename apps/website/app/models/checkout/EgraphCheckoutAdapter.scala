package models.checkout

import forms._
import forms.ShippingAddress
import services.AppConfig
import services.db.HasTransientServices
import models.{AccountStore, CustomerStore, Customer, Account}
import com.google.inject.Inject
import services.http.ServerSession
import play.api.mvc.{Request, RequestHeader}
import services.ecommerce.CartFactory
import controllers.api.checkout.CheckoutEndpoints._

case class CheckoutAdapterServices @Inject() (
  accountStore: AccountStore,
  customerStore: CustomerStore,
  cartFactory: CartFactory
) {
  def decacheOrCreate(celebId: Long)(implicit request: RequestHeader): EgraphCheckoutAdapter = {
    decache(celebId) getOrElse EgraphCheckoutAdapter(celebId).cache() // TODO(CE-13): cache asynchronously, perhaps?
  }

  def decache(celebId: Long)(implicit request: RequestHeader): Option[EgraphCheckoutAdapter] = {
    cartFactory(celebId).get[EgraphCheckoutAdapter]("checkout")
  }
}

/**
 * Basically, a cacheable form of the Checkout that makes it easy to add, replace, and remove specific components of
 * the checkout.
 *
 * The actual checkout is not serializable currently and manipulating its contents is more verbose due to its
 * generalized nature. This class may be extended to cover other purchase scenarios or just ditched if Checkouts are
 *
 *
 * @param order - to be transacted, required
 * @param coupon - to be applied to checkout
 * @param payment - to be charged if balance is nonZero
 * @param buyerEmail - email of buyer, required
 * @param recipientEmail - email of giftee, required if order is a gift
 * @param shippingAddress - shipping address for print, required if order is physical
 * @param _services
 */
case class EgraphCheckoutAdapter (
  celebId: Long,
  // named after the api endpoints they correspong to
  order: Option[EgraphOrderLineItemType] = None,
  coupon: Option[CouponLineItemType] = None,
  payment: Option[CashTransactionLineItemType] = None,
  buyerEmail: Option[String] = None,
  recipientEmail: Option[String] = None,
  shippingAddress: Option[ShippingAddress] = None,
  @transient _services: CheckoutAdapterServices = AppConfig.instance[CheckoutAdapterServices]
) extends HasTransientServices[CheckoutAdapterServices] {

  //
  // Members
  //
  def buyer = CheckoutBuyer(buyerEmail)

  def recipient = recipientEmail map { email => CheckoutRecipient(Some(email)) }

  def cart(implicit request: RequestHeader): ServerSession = services.cartFactory(celebId)

  def cache()(implicit request: RequestHeader) = {
    cart.setting("checkout" -> this).save()
    this
  }

  def formState(implicit request: Request[_]) = {
    import play.api.libs.json._

    def formJson(checkoutForm: CheckoutForm[_]) = {
      checkoutForm.decache(this).map(form => Json.toJson(form.data))
    }
    Json.obj(
      "egraph" -> formJson(EgraphForm),
      "recipient" -> formJson(RecipientForm),
      "buyer" -> formJson(BuyerForm),
      "shippingAddress" -> formJson(ShippingAddressForm),
      "payment" -> formJson(PaymentForm),
      "coupon" -> formJson(CouponForm)
    )
  }

  //
  // Checkout methods
  //
  def summary = previewCheckout.toJson

  def transact() = if (validated) Some(makeActualCheckout().transact(payment)) else None


  //
  // Setters
  //
  def withOrder(order: Option[EgraphOrderLineItemType] = None) = this.copy(order = order)
  def withCoupon(coupon: Option[CouponLineItemType] = None) = this.copy(coupon = coupon)
  def withPayment(payment: Option[CashTransactionLineItemType] = None) = this.copy(payment = payment)
  def withBuyerEmail(buyer: Option[String] = None) = this.copy(buyerEmail = buyer)
  def withRecipientEmail(recipient: Option[String] = None) = this.copy(recipientEmail = recipient)
  def withShippingAddress(shippingAddress: Option[ShippingAddress] = None) = this.copy(shippingAddress = shippingAddress)


  //
  // helpers
  //
  protected def isGift = order map (_.isGift) getOrElse (false)

  protected def hasPrint = order map (_.framedPrint) getOrElse (false)

  /** if true, the actual checkout should be ready to transact */
  protected def validated = {
    val checkout = previewCheckout

    printValidationStatus(checkout)

    order.isDefined &&
    buyerEmail.isDefined &&
    (recipientEmail.isDefined || !isGift) &&
    (shippingAddress.isDefined || !hasPrint) &&
    (payment.isDefined || previewCheckout.total.amount.isZero)
  }
  
  protected def printValidationStatus(checkout: Checkout) {
    log(s"""" +
       Validating order before purchase:
         has order? ${order.isDefined}
         has buyerEmail? ${buyerEmail.isDefined}
         has recipientEmail? ${recipientEmail.isDefined}
           isGift? ${isGift}
         has shippingAddress? ${shippingAddress.isDefined}
           hasPrint? ${hasPrint}
         has payment? ${payment.isDefined}
           total: ${checkout.total.amount}
     """)
  }

  /** generate a Checkout without saving any Customers or Accounts */
  protected def previewCheckout = {
    val types: Seq[LineItemType[_]] = Seq(order, coupon).flatten
    println("Types are -- " + types.map(_.codeType))
    val zipcode = payment.flatMap(_.billingPostalCode)
    val address = shippingAddress.map(_.address)
    val recipientAccount = recipient map (_.account)

    Checkout.create( types, Some(buyer.account), zipcode)
      .withRecipient( recipientAccount )
      .withShippingAddress( address )
  }

  /** try to generate a Checkout ready to transact -- should be able to transact if `validated` is true */
  protected def makeActualCheckout(): FreshCheckout = previewCheckout
    .withRecipient { recipient map (_.account.save()) }
    .withBuyer { buyer.account.save() }


  /** Helper classes for dealing with Account and Customer related business for buyer and recipient */
  protected case class CheckoutBuyer(email: Option[String]) extends CheckoutCustomer(isGiftee = false)


  protected case class CheckoutRecipient(email: Option[String]) extends CheckoutCustomer(isGiftee = true)


  protected abstract class CheckoutCustomer(isGiftee: Boolean) {
    def email: Option[String]

    def customer: Customer = getOrCreate(
      existing = email flatMap {services.customerStore.findByEmail(_)} )(
      create = account.createCustomer(name)
    )

    def name: String = {
      def nameFromShipping = if (isGiftee) None else shippingAddress map { _.name } // buyer name would be on shipping addres
      def nameFromOrder = if (!isForMe) None else order map { _.recipientName }
      def nameFromEmail = email map { _.split('@').head }

      getOrCreate(nameFromOrder, nameFromShipping, nameFromEmail) ("")
    }

    def account: Account = getOrCreate(
      existing = email flatMap { services.accountStore.findByEmail(_) } )(
      create = Account( email = email.getOrElse("") )
    )

    private def isForMe = !isGift || isGiftee
    private def getOrCreate[T](existing: Option[T]*)(create: => T): T = existing.flatten.headOption getOrElse create
  }
}
