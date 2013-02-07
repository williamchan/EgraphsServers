package models.checkout

import java.sql.{Connection, Timestamp}
import models._
import checkout.Conversions._
import services.AppConfig



/** NOTE(CE-13): this checkout instance needs to be serializable */
case class FreshCheckout(
  _itemTypes: LineItemTypes,

  _buyer: Option[Customer] = None,
  recipient: Option[Customer] = None,
  shippingAddress: Option[Address] = None,
  stripeToken: Option[String] = None,

  zipcode: Option[String] = None,
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // CE-13 Changes
  //
  override def buyer = _buyer.getOrElse(Customer())
  override def payment = stripeToken map (token => CashTransactionLineItemType(Some(token), zipcode))


  //
  // Checkout members
  //
  override def _entity = CheckoutEntity(0, buyer.id)
  override lazy val itemTypes: LineItemTypes = summaryTypes ++ (_derivedTypes ++ _itemTypes)
  override lazy val lineItems: LineItems = resolveTypes(itemTypes)
  override def pendingTypes: LineItemTypes = _itemTypes
  override def pendingItems: LineItems = lineItems
  override protected def _dirty: Boolean = !_itemTypes.isEmpty


  //
  // Checkout methods
  //
  override def withAdditionalTypes(newTypes: LineItemTypes): FreshCheckout = copy(_itemTypes = newTypes ++ _itemTypes)
  override def withSavedEntity(savedEntity: CheckoutEntity): PersistedCheckout = PersistedCheckout(savedEntity)

  override def save() = this.withSavedBuyerAndRecipient().insert()


  //
  // Helper methods
  //
  def withRecipient(newRecipient: Option[Customer]) = this.copy(recipient = newRecipient)
  def withBuyer(newBuyer: Customer) = this.copy(_buyer = Some(newBuyer))
  def withZipcode(newZipcode: Option[String]) = this.copy(zipcode = newZipcode)
  def withShippingAddress(newAddress: Option[Address]) = this.copy(shippingAddress = newAddress)

  private def withSavedBuyerAndRecipient() = withRecipient (recipient map { _.save() }) withBuyer (buyer.save())
}




