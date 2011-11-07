package models

import libs.Time
import java.sql.Timestamp
import db.{Schema, Saves, KeyedCaseClass}

case class Product(
  id: Long = 0L,
  celebrityId: Int = 0,
  priceInCents: Int = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  override def unapplied = Product.unapply(this)
}

object Product extends Saves[Product] with SavesCreatedUpdated[Product] {
  def table = Schema.products
  
  override def withCreatedUpdated(toUpdate: Product, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  override def defineUpdate(theOld: Product, theNew: Product) = {
    import org.squeryl.PrimitiveTypeMode._
    
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.priceInCents := theNew.priceInCents,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }
}

