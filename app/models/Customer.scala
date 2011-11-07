package models

import java.sql.Timestamp
import libs.Time
import org.squeryl.PrimitiveTypeMode._
import db.{KeyedCaseClass, Schema, Saves}

/**
 * Persistent entity representing customers who buy products from our service.
 */
case class Customer(
  id: Long = 0L,
  accountId: Long = 0L,
  name: Option[String] = Some(""),
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  override def unapplied = Customer.unapply(this)
}

object Customer extends Saves[Customer] with SavesCreatedUpdated[Customer] {
  //
  // Saves[Customer] methods
  //
  override val table = Schema.customers

  override def defineUpdate(theOld: Customer, theNew: Customer) = {
    updateIs(
      theOld.accountId := theNew.accountId,
      theOld.name  := theNew.name,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Customer] methods
  //
  override def withCreatedUpdated(toUpdate: Customer, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}