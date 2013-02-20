package models.checkout

import services.Time
import services.db.KeyedCaseClass
import models.{Customer, HasCreatedUpdated}
import java.sql.Timestamp

//
// Entity
//
case class CheckoutEntity(
  id: Long = 0,
  customerId: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = CheckoutEntity.unapply(this)

  def withCustomer(customer: Customer) = this.copy(customerId = customer.id)
}
