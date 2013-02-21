package models.checkout

import forms.{ShippingAddress}
import services.AppConfig
import services.db.HasTransientServices
import models.{Customer, Account}


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
  def isGift = order map (_.isGift) getOrElse (false)

  def isPhysical = order map (_.framedPrint) getOrElse (false)

  def buyer = new CheckoutCustomer(buyerEmail, isGiftee = false)

  def recipient = recipientEmail map { email =>
    new CheckoutCustomer(Some(email), isGiftee = true)
  }


  //
  // Checkout methods
  //
  def summary = previewCheckout.toJson

  def previewCheckout = {
    val types: Seq[LineItemType[_]] = Seq(order, coupon).flatten
    val zipcode = payment.flatMap(_.billingPostalCode)
    val address = shippingAddress.map(_.address)
    val recipientAccount = recipient map (_.account)

    Checkout.create( types, Some(buyer.account), zipcode)
      .withRecipient( recipientAccount )
      .withShippingAddress( address )
  }

  def validated = {
    order.isDefined &&
      buyerEmail.isDefined &&
      (recipientEmail.isDefined || !isGift) &&
      (shippingAddress.isDefined || !isPhysical) &&
      (payment.isDefined || previewCheckout.total.amount.isZero)
  }

  def checkout(): FreshCheckout = previewCheckout
    .withRecipient { recipient map (_.account.save()) }
    .withBuyer { buyer.account.save() }

  def transact() = checkout.transact(payment)


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
  // privates
  //
  /** Helper class for dealing with Account and Customer related business for buyer and recipient */
  protected class CheckoutCustomer(val email: Option[String], isGiftee: Boolean) {

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
