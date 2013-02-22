package models.checkout

import java.sql.{Connection, Timestamp}
import models._
import checkout.Conversions._
import services.AppConfig
import scalaz.Lens
import services.db.{CanInsertAndUpdateEntityThroughServices, HasTransientServices}

/**
 * For now, requires a saved Account for the buyer to simplify some things. Want to remove this requirement
 * eventually to make it possible for creating a cart to preview without creating an account.
 */
case class FreshCheckout(
  id: Long = 0L,
  _itemTypes: LineItemTypes,
  _buyerAccount: Option[Account],
  recipientAccount: Option[Account] = None,
  shippingAddress: Option[Address] = None,
  stripeToken: Option[String] = None,
  zipcode: Option[String] = None,
  @transient _services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // Checkout members
  //
  override lazy val _entity = CheckoutEntity(id, buyerCustomer.id)

  /** all `LineItemType`s */
  override lazy val itemTypes: LineItemTypes = summaryTypes ++ (_derivedTypes ++ _itemTypes)

  /** all `LineItem`s */
  override lazy val lineItems: LineItems = resolveTypes(itemTypes)

  /** `LineItemType`s of items that are to be transacted (all of them) */
  override def pendingTypes: LineItemTypes = _itemTypes

  /** `LineItem`s to be transacted (all of them) */
  override def pendingItems: LineItems = lineItems

  override lazy val buyerCustomer: Customer = services.customerStore.findOrCreateByEmail(buyerAccount.email)

  override lazy val buyerAccount: Account = _buyerAccount getOrElse {
    throw new Exception("Attempting operation requiring buyer without defining buyer first.")
  }

  override lazy val recipientCustomer: Option[Customer] = recipientAccount map { account =>
    services.customerStore.findOrCreateByEmail(account.email)
  }

  override def payment = for (token <- stripeToken; zip <- zipcode) yield CashTransactionLineItemType.create(token, zip)

  override protected def _dirty: Boolean = !_itemTypes.isEmpty


  //
  // Checkout methods
  //
  override def withAdditionalTypes(newTypes: LineItemTypes): FreshCheckout = copy(_itemTypes = newTypes ++ _itemTypes)
  override def withEntity(entity: CheckoutEntity): FreshCheckout = this.copy(entity.id)

  override def save() = {
    this.withShippingAddress(savedShippingAddress).insert()
  }


  //
  // Helper methods
  //
  def withBuyer(newBuyer: Account) = this.copy(_buyerAccount = Some(newBuyer))
  def withRecipient(newRecipient: Option[Account]) = this.copy(recipientAccount = newRecipient)
  def withZipcode(newZipcode: Option[String]) = this.copy(zipcode = newZipcode)
  def withShippingAddress(newAddress: Option[Address]) = this.copy(shippingAddress = newAddress)

  private def savedIfDefined(maybeAccount: Option[Account]) = maybeAccount map (_.id > 0) getOrElse true

  private def savedShippingAddress() = shippingAddress map { address =>
    address.copy(accountId = buyerAccount.id).save()
  }

  private def customerId = Lens[FreshCheckout, Long](
    get = _ => _entity.customerId,
    set = (checkout, newId) => checkout.withEntity(_entity.copy(customerId = newId))
  )
}