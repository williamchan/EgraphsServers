package models.checkout

import java.sql.{Connection, Timestamp}
import models._
import models.enums._
import checkout.Conversions._
import services.AppConfig



case class PersistedCheckout(
  _entity: CheckoutEntity,
  _addedTypes: LineItemTypes = Nil,
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // Checkout members
  //
  override def isPersisted: Boolean = true

  override lazy val customer: Option[Customer] = services.customerStore.findById(customerId)

  override lazy val zipcode: Option[String] = {
    _lineItems(CodeType.CashTransaction).headOption flatMap (_.domainObject.billingPostalCode)
  }

  /** all LineItemTypes */
  override lazy val itemTypes: LineItemTypes = summaryTypes ++ (pendingTypes ++ _lineItems.map(_.itemType))

  /** all LineItems */
  override lazy val lineItems: LineItems = resolveTypes(summaryTypes, pendingItems ++ _lineItems)

  /** LineItems that need to be transacted in next transaction */
  override lazy val pendingItems: LineItems = {
    // resolve added types against existing items, then filter out _lineItems
    val addedItems = resolveTypes(_addedTypes, _lineItems) filter (_addedTypes contains _.itemType)
    val derivedTypesWithTempSummaries = summaryTypes ++ _derivedTypes // NOTE(SER-499): somewhat not happy about this
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

  override def withSavedEntity(savedEntity: CheckoutEntity): PersistedCheckout = {
    this.copy(savedEntity)
  }


  //
  // PersistedCheckout methods and member
  //
  protected lazy val _lineItems: LineItems = services.lineItemStore.getItemsByCheckoutId(id)
}
