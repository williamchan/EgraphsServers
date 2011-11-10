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
  name: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  def save(): Customer = {
    Customer.save(this)
  }

  /**
   * Orders an Egraph product on behalf of a recipient.
   *
   * @param product the Product to order
   * @param recipient the Customer receiving the eGraph. Defaults to the purchasing Customer.
   *
   * @return an Order for the Product, purchased by this Customer for the recipient Customer.
   */
  def order(product: Product, recipient: Customer = this): Order = {
    Order(
      buyerId=id,
      recipientId=recipient.id,
      productId=product.id,
      amountPaidInCents=product.priceInCents
    )
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Customer.unapply(this)
  }
}

object Customer extends Saves[Customer] with SavesCreatedUpdated[Customer] {
  //
  // Saves[Customer] methods
  //
  override val table = Schema.customers

  override def defineUpdate(theOld: Customer, theNew: Customer) = {
    updateIs(
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