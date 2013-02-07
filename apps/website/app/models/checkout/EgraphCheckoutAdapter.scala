package models.checkout

import data.{CheckoutShippingAddress, CustomerEmail}
import play.api.libs.json.JsNull
import services.AppConfig
import services.db.HasTransientServices
import models.{Customer, Account}
import models.checkout.checkout._


case class EgraphCheckoutAdapter (
  // named after the api endpoints they correspong to
  order: Option[EgraphOrderLineItemType] = None,
  coupon: Option[CouponLineItemType] = None,
  payment: Option[CashTransactionLineItemType] = None,
  _buyer: Option[CustomerEmail] = None,
  _recipient: Option[CustomerEmail] = None,
  shippingAddress: Option[CheckoutShippingAddress] = None,
  @transient _services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends HasTransientServices[CheckoutServices] {

  //
  // Members
  //
  def isGift = order map (_.isGift) getOrElse (false)

  def buyer = new CheckoutCustomer(_buyer, isGiftee = false)

  def recipient = new CheckoutCustomer(_recipient, isGiftee = true)

  //
  // Checkout methods
  //
  def summary = checkout.toJson

  def valid: Boolean = { false }

  def transact() = checkout.transact(payment)

  def checkout: FreshCheckout = {
    val types: Seq[LineItemType[_]] = Seq(order, coupon).flatten
    val zipcode = payment.flatMap(_.billingPostalCode)
    val address = shippingAddress.map(_.address)

    Checkout.create(types, Some(buyer.customer), zipcode)
      .withRecipient( Some(recipient.customer) )
      .withShippingAddress( address )
  }



  //
  // Setters
  //
  def withOrder(order: Option[EgraphOrderLineItemType] = None) = this.copy(order = order)
  def withCoupon(coupon: Option[CouponLineItemType] = None) = this.copy(coupon = coupon)
  def withPayment(payment: Option[CashTransactionLineItemType] = None) = this.copy(payment = payment)
  def withBuyer(buyer: Option[CustomerEmail] = None) = this.copy(_buyer = buyer)
  def withRecipient(recipient: Option[CustomerEmail] = None) = this.copy(_recipient = recipient)
  def withShippingAddress(shippingAddress: Option[CheckoutShippingAddress] = None) = this.copy(shippingAddress = shippingAddress)


  //
  // privates
  //
  /** Helper class for dealing with Account and Customer related business for buyer and recipient */
  protected class CheckoutCustomer(customerEmail: Option[CustomerEmail], isGiftee: Boolean) {
    def email: Option[String] = customerEmail map (_.email)

    def customer: Customer = getOrCreate(
      existing = email flatMap {services.customerStore.findByEmail(_)} )(
      create = account.createCustomer(name)
    )

    def name: String = {
      def nameFromShipping = if (isGiftee) None else shippingAddress map { _.name }
      def nameFromOrder = if (!isForMe) None else order map { _.recipientName }
      def nameFromEmail = email map { _.split('@').head }

      getOrCreate(nameFromShipping, nameFromOrder, nameFromEmail) ("")
    }

    def account: Account = getOrCreate(
      existing = email flatMap { services.accountStore.findByEmail(_) } )(
      create = Account( email = email.getOrElse("") )
    )

    private def isForMe = isGift == isGiftee
    private def getOrCreate[T](existing: Option[T]*)(create: => T): T = existing.flatten.headOption getOrElse create
  }
}
