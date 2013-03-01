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
 * @param buyerDetails - email of buyer, required
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
  buyerDetails: Option[BuyerDetails] = None,
  recipientEmail: Option[String] = None,
  shippingAddress: Option[ShippingAddress] = None,
  @transient _services: CheckoutAdapterServices = AppConfig.instance[CheckoutAdapterServices]
) extends HasTransientServices[CheckoutAdapterServices] {

  //
  // Members
  //
  def buyer = new CheckoutBuyer(buyerDetails)

  def recipient = recipientEmail map { email => new CheckoutRecipient(Some(email)) }

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

  /**
   * @return None if the checkout's constituent API resources had not been provided, Some(Left(_)) if transacting
   *        the checkout failed in some way, Some(Right(_)) if transacting succeeded
   */
  def transact() = if (validated) Some(makeActualCheckout().transact(payment)) else None


  //
  // Setters
  //
  def withOrder(order: Option[EgraphOrderLineItemType] = None) = this.copy(order = order)
  def withCoupon(coupon: Option[CouponLineItemType] = None) = this.copy(coupon = coupon)
  def withPayment(payment: Option[CashTransactionLineItemType] = None) = this.copy(payment = payment)
  def withBuyer(buyer: Option[BuyerDetails] = None) = this.copy(buyerDetails = buyer)
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
    buyerDetails.isDefined &&
    (recipientEmail.isDefined || !isGift) &&
    (shippingAddress.isDefined || !hasPrint) &&
    (payment.isDefined || previewCheckout.total.amount.isZero)
  }
  
  protected def printValidationStatus(checkout: Checkout) {
    log(s"""" +
       Validating order before purchase:
         has order? ${order.isDefined}
         has buyer? ${buyerDetails.isDefined}
           has name? ${buyerDetails map (_.name.isDefined) getOrElse (false)}
         has recipientEmail? ${recipientEmail.isDefined}
           isGift? ${isGift}
         has shippingAddress? ${shippingAddress.isDefined}
           hasPrint? ${hasPrint}
         has payment? ${payment.isDefined}
           total: ${checkout.total.amount}
     """)
  }

  /** generate a Checkout without saving any Customers or Accounts */
  protected[checkout] def previewCheckout = {
    val types: Seq[LineItemType[_]] = Seq(order, coupon).flatten
    val zipcode = payment.flatMap(_.billingPostalCode)
    val address = shippingAddress.map(_.stringify)
    val recipientAccount = recipient map (_.account)
    val recipientCustomer = recipient map (_.customer)

    Checkout.create( types )
      .withBuyerAccount( buyer.account )
      .withBuyerCustomer( buyer.customer )
      .withRecipientAccount( recipientAccount )
      .withRecipientCustomer( recipientCustomer)
      .withZipcode( zipcode )
      .withShippingAddress( address )
  }

  /** try to generate a Checkout ready to transact -- should be able to transact if `validated` is true */
  protected def makeActualCheckout(): FreshCheckout = {
    val (buyerCustomer, buyerAccount) = buyer.savedCustomerAndAccount()
    val (recipientCustomer, recipientAccount) = recipient match {
      case None => (None, None)
      case Some(checkoutRecipient) =>
        val (customer, recip) = checkoutRecipient.savedCustomerAndAccount()
        (Some(customer), Some(recip))
    }

    previewCheckout
      .withBuyerAccount( buyerAccount )
      .withBuyerCustomer( buyerCustomer )
      .withRecipientAccount( recipientAccount )
      .withRecipientCustomer( recipientCustomer)
  }


  /** Helper classes for dealing with Account and Customer related business for buyer and recipient */
  protected class CheckoutBuyer(details: Option[BuyerDetails]) extends CheckoutCustomer(details, isGiftee = false)
  protected class CheckoutRecipient(email: Option[String]) extends CheckoutCustomer(email map (BuyerDetails(None, _)), isGiftee = true)

  protected abstract class CheckoutCustomer(details: Option[BuyerDetails], isGiftee: Boolean) {
    def email = details map (_.email)

    def name  = {
      def fromDetails = details flatMap { _.name }
      def fromOrder = optionIf(isForMe) { order map (_.recipientName) } flatten

      getOrCreate(fromDetails orElse fromOrder, "")
    }

    def customer: Customer = getOrCreate(
      existing = maybeExistingCustomer,
      create = account.createCustomer(name)
    )

    def account: Account = getOrCreate(
      existing = maybeExistingAccount,
      create = Account( email = email.getOrElse("") )
    )

    def savedCustomerAndAccount() = {
      val savedCustomer = getOrCreate(
        existing = maybeExistingCustomer,
        create = account.createCustomer(name).save()
      )
      val savedAccount = account.copy(customerId = Some(savedCustomer.id)).save()

      (savedCustomer, savedAccount)
    }

    private def isForMe = !isGift || isGiftee
    private def getOrCreate[T](existing: Option[T], create: => T): T = existing getOrElse create

    private def maybeExistingAccount = email flatMap { services.accountStore.findByEmail(_) }
    private def maybeExistingCustomer = email flatMap { services.customerStore.findByEmail(_) }
  }
}
