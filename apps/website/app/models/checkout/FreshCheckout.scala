package models.checkout

import java.sql.{Connection, Timestamp}
import models._
import checkout.Conversions._
import services.AppConfig
import scala.Some



case class FreshCheckout(
  _entity: CheckoutEntity,
  _itemTypes: LineItemTypes,
  customer: Option[Customer],
  zipcode: Option[String],
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // Checkout members
  //
  override lazy val itemTypes: LineItemTypes = summaryTypes ++ (_derivedTypes ++ _itemTypes)
  override lazy val lineItems: LineItems = resolveTypes(itemTypes)
  override def pendingTypes: LineItemTypes = _itemTypes
  override def pendingItems: LineItems = lineItems
  override def isPersisted: Boolean = false


  //
  // Checkout methods
  //
  override def withAdditionalTypes(newTypes: LineItemTypes): FreshCheckout = copy(_itemTypes = newTypes ++ _itemTypes)
  override def withSavedEntity(savedEntity: CheckoutEntity): PersistedCheckout = PersistedCheckout(savedEntity)


  //
  // Helper methods
  //
  def withCustomer(newCustomer: Customer): FreshCheckout = this.copy(customer = Some(newCustomer))
  def withZipcode(newZipcode: String): FreshCheckout = this.copy(zipcode = Some(newZipcode))
}
