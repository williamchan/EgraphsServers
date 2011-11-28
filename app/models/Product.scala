package models

import libs.Time
import java.sql.Timestamp
import db.{Schema, Saves, KeyedCaseClass}
import org.squeryl.Query

/**
 * An item on sale by a Celebrity. In the case of the base Egraph, it represents a signature service
 * against a particular photograph of the celebrity.
 */
case class Product(
  id: Long = 0L,
  celebrityId: Long = 0L,
  priceInCents: Int = 0,
  name: String = "",
  description: String = "",
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
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //
  /** Locates all of the products being sold by a particular celebrity */
  def findByCelebrity(celebrityId: Long): Query[Product] = {
    from(Schema.products)(product =>
      where(product.celebrityId === celebrityId)
      select(product)
    )
  }

  //
  // Saves[Product] methods
  //
  def table = Schema.products

  override def defineUpdate(theOld: Product, theNew: Product) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.priceInCents := theNew.priceInCents,
      theOld.name := theNew.name,
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