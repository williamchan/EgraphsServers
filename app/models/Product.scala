package models

import libs.Time
import java.sql.Timestamp
import db.{Schema, Saves, KeyedCaseClass}

case class Product(
  id: Long = 0L,
  celebrityId: Long = 0L,
  priceInCents: Int = 0,
  description: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  //
  // Public methods
  //
  def save(): Product = {
    Product.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Product.unapply(this)
  }
}

object Product extends Saves[Product] with SavesCreatedUpdated[Product] {
  //
  // Saves[Product] methods
  //
  def table = Schema.products

  override def defineUpdate(theOld: Product, theNew: Product) = {
    import org.squeryl.PrimitiveTypeMode._
    
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.priceInCents := theNew.priceInCents,
      theOld.description := theNew.description,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Product] methods
  //
  override def withCreatedUpdated(toUpdate: Product, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

}

