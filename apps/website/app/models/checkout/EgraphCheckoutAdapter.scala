package models.checkout

import forms.{ShippingAddress}
import services.AppConfig
import services.db.HasTransientServices
import models.{Customer, Account}

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
  // named after the api endpoints they correspong to
  order: Option[EgraphOrderLineItemType] = None,
  coupon: Option[CouponLineItemType] = None,
  payment: Option[CashTransactionLineItemType] = None,
  buyerEmail: Option[String] = None,
  recipientEmail: Option[String] = None,
  shippingAddress: Option[ShippingAddress] = None,
  @transient _services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends HasTransientServices[CheckoutServices] {

  //
  // Members
  //
  def buyer = CheckoutBuyer(buyerEmail)
  def recipient = recipientEmail map { email => CheckoutRecipient(Some(email)) }


  //
  // Checkout methods
  //
  def summary = previewCheckout.toJson

  /** if true, the actual checkout should be ready to transact */
  def validated = { order.isDefined &&
    buyerEmail.isDefined &&
    (recipientEmail.isDefined || !isGift) &&
    (shippingAddress.isDefined || !hasPrint) &&
    (payment.isDefined || previewCheckout.total.amount.isZero)
  }

  def transact() = makeActualCheckout().transact(payment)


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

  /** generate a Checkout without saving any Customers or Accounts */
  protected def previewCheckout = {
    val types: Seq[LineItemType[_]] = Seq(order, coupon).flatten
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
