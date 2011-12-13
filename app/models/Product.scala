package models

import libs.Time
import java.sql.Timestamp
import org.squeryl.Query
import db.{FilterOneTable, Schema, Saves, KeyedCaseClass}
import play.templates.JavaExtensions
import org.joda.money.{CurrencyUnit, Money}
import libs.Finance.TypeConversions._

/**
 * An item on sale by a Celebrity. In the case of the base Egraph, it represents a signature service
 * against a particular photograph of the celebrity.
 */
case class Product(
  id: Long = 0L,
  celebrityId: Long = 0L,
  priceInCurrency: BigDecimal = 0,
  name: String = "",
  description: String = "",
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  //
  // Additional DB columns
  //
  /** The slug used to access this product from the main site */
  val urlSlug = JavaExtensions.slugify(name, false) // Slugify without lower-casing

  //
  // Public members
  //
  def save(): Product = {
    Product.save(this)
  }

  def price: Money = {
    priceInCurrency.toMoney()
  }

  def withPrice(money: Money) = {
    copy(priceInCurrency=BigDecimal(money.getAmount))
  }

  def withPrice(money: BigDecimal) = {
    copy(priceInCurrency=money)
  }

  /** Retrieves the celebrity from the database */
  def celebrity: Celebrity = {
    Celebrity.get(celebrityId)
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
  object FindByCelebrity {
    def apply(celebrityId: Long, filters: FilterOneTable[Product] *): Query[Product] = {
      from(Schema.products)(product =>
        where(
          product.celebrityId === celebrityId and
          FilterOneTable.reduceFilters(filters, product)
        )
          select(product)
      )
    }

    object Filters {
      case class WithUrlSlug(slug: String) extends FilterOneTable[Product] {
        override def test(product: Product) = {
          product.urlSlug === slug
        }
      }
    }
  }

  //
  // Saves[Product] methods
  //
  def table = Schema.products

  override def defineUpdate(theOld: Product, theNew: Product) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.priceInCurrency := theNew.priceInCurrency,
      theOld.name := theNew.name,
      theOld.urlSlug := theNew.urlSlug,
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