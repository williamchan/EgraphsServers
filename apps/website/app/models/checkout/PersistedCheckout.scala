package models.checkout

import java.sql.{Connection, Timestamp}
import models._
import models.enums._
import Conversions._
import services.AppConfig
import services.db.{CanInsertAndUpdateEntityThroughServices, HasTransientServices}


case class PersistedCheckout(
  _entity: CheckoutEntity,
  _addedTypes: LineItemTypes = Nil,
  stripeToken: Option[String] = None,      // used for payment or refund
  zipcode: Option[String] = None,
  @transient _services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // CE-13 Changes
  //
  override def id = _entity.id
  override lazy val buyerCustomer: Customer = services.customerStore.findById(_entity.customerId).head
  override lazy val buyerAccount: Account = buyerCustomer.account

  // todo: provide real implementation when these are needed after point of transaction
  override def recipientAccount: Option[Account] = None
  override def recipientCustomer: Option[Customer] = None

  /** cash transaction to be made if changes are transacted */
  override def payment: Option[CashTransactionLineItemType] = {
    for ( token <- stripeToken; zip <- zipcode) yield
      CashTransactionLineItemType.create(token, zip)
  }

  override lazy val shippingAddress = {
    for (print <- lineItems(CheckoutCodeType.PrintOrder).headOption) yield print.domainObject.shippingAddress
  }


  //
  // Checkout members
  //
  override protected def _dirty: Boolean = !_addedTypes.isEmpty

  /** all LineItemTypes */
  override lazy val itemTypes: LineItemTypes = summaryTypes ++ (pendingTypes ++ _lineItems.map(_.itemType))

  /** all LineItems */
  override lazy val lineItems: LineItems = resolveTypes(summaryTypes, pendingItems ++ _lineItems)

  /** LineItems that need to be transacted in next transaction */
  override lazy val pendingItems: LineItems = {
    // resolve added types against existing items, then filter out _lineItems
    val addedTypes = _addedTypes.toSet

    // TODO: coupons currently don't work on `PersistedCheckout`s because they need a subtotal to be applied against
    val addedItems = resolveTypes(_addedTypes, _lineItems) filter (addedTypes contains _.itemType)

    // temp summaries needed for some types to resolve...
    val derivedTypesWithTempSummaries = summaryTypes ++ _derivedTypes

    resolveTypes(derivedTypesWithTempSummaries, addedItems).notOfNature(LineItemNature.Summary)
  }

  /** Superset of the LineItemTypes for pendingItems */
  override lazy val pendingTypes: LineItemTypes = _derivedTypes ++ _addedTypes


  //
  // Checkout methods
  //
  override def withAdditionalTypes(newTypes: LineItemTypes): PersistedCheckout = {
    copy(_addedTypes = newTypes ++ _addedTypes)
  }

  override def withEntity(entity: CheckoutEntity): PersistedCheckout = this.copy(entity)

  override def save(): Checkout = this.update()


  //
  // PersistedCheckout methods and member
  //
  /** restored items from db */
  protected lazy val _lineItems: LineItems = services.lineItemStore.getItemsByCheckoutId(id)
}