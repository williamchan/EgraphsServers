package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums._
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db._
import models.{HasCreatedUpdated, SavesCreatedUpdated}
import com.google.inject.Inject

package object checkout {
  /** For _domainEntityId if domain object doesn't get persisted; ex: Tax */
  val UnusedDomainEntity: Long = -1

  /** For id's of line items and line item types that don't get persisted; ex: Subtotal */
  val Unpersisted: Long = -2

  val UnsavedEntity = 0

  type LineItems = Seq[LineItem[_]]
  type LineItemTypes = Seq[LineItemType[_]]
}

import checkout._

case class CheckoutEntity(
  id: Long = 0,
  customerId: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = CheckoutEntity.unapply(this)
}

case class CheckoutServices @Inject() (checkoutStore: CheckoutStore)




case class Checkout (
  _entity: CheckoutEntity,
  _typesOrItems: Either[LineItemTypes, LineItems],
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) {

  // uses LineItems and LineItemTypes
  def lineItems: LineItems =  _typesOrItems match {
    case Right(items: LineItems) => items
    case Left(types: LineItemTypes) => {
      // TODO(SER-499): clean this bessy up
      case class ResolutionPass(items: LineItems, unresolved: LineItemTypes) {
        def isComplete: Boolean = unresolved.isEmpty
      }

      def executePass(passToResolve: ResolutionPass): LineItems = {
        if (passToResolve.isComplete) {
          passToResolve.items
        } else {
          val resolvedPass = passToResolve.unresolved.foldLeft(passToResolve) { (oldPass, nextItemType) =>
            val itemTypesSansCurrent = oldPass.unresolved.filter(_ != nextItemType)

            nextItemType.lineItems(oldPass.items, itemTypesSansCurrent) match {
              case Nil => oldPass
              case newLineItems: LineItems => oldPass.copy(newLineItems, itemTypesSansCurrent)
            }
          }

          // TODO(SER-499): are assertions disabled in prod?
          // Check to make sure we're not in a circular dependency loop
          assert(
            resolvedPass.unresolved.length != passToResolve.unresolved.length,
            "Attempt to resolve LineItemTypes to LineItems failed to resolve even one: " +
              passToResolve.items +
              "\n\n" +
              passToResolve.unresolved
          )

          executePass(resolvedPass)
        }
      }

      val initialPass = ResolutionPass(IndexedSeq(), lineItemTypes)
      executePass(initialPass)
    }
  }

  def lineItemTypes: LineItemTypes = _typesOrItems match {
    case Left(types: LineItemTypes) => types
    case Right(items: LineItems) => items.map(_.itemType)
  }


  /**
   * Persists this checkout, its types, and its items
   * @return PersistedCheckout of the transacted checkout
   */
  def transact(): Checkout = {
    // TODO(SER-499): not happy with this
    // save _entity, lineItemTypes, and lineItems
    services.checkoutStore.create(this)
  }


  // utility members
  def id = _entity.id
  def subtotal: SubtotalLineItem = lineItemsOfCodeType(CodeType.Subtotal).head
  def total: TotalLineItem = lineItemsOfCodeType(CodeType.Total).head

  def flattenLineItems: LineItems = {
    for (lineItem <- lineItems; flattened <- lineItem.flatten) yield flattened
  }

  def lineItemsOfCodeType[LIT <: LineItemType[_], LI <: LineItem[_]](codeType: CodeTypeFactory[LIT, LI]): Seq[LI] = {
    for (item <- lineItems if item.codeType == codeType) yield {
      item.asInstanceOf[LI]
    }

  }
}

object Checkout {
  import checkout._

  // Create
  def apply(types: LineItemTypes, zipcode: String) = {
    val maybeZipcode = if (zipcode.isEmpty) None else Some(zipcode)
    val typesWithTaxes = types ++ taxesAndSummariesByZip(maybeZipcode)

    new Checkout(CheckoutEntity(), Left(typesWithTaxes))
  }

  // Restore
  def apply(entity: CheckoutEntity, items: LineItems) = {
    assert(!items.exists(item => item.checkoutId != entity.id))
    new Checkout(entity, Right(itemsWithSummaries(items)))
  }


  /**
   * Helper for generating taxes and summaries.
   * TODO: add fees, such as shipping, later.
   *
   * @param maybeZipcode - None defaults to no taxes
   * @return Sequence of subtotal, taxes, and total (in that order)
   */
  protected def taxesAndSummariesByZip(maybeZipcode: Option[String]): LineItemTypes = {
    Seq(SubtotalLineItemType) ++
      TaxLineItemType.getTaxesByZip(maybeZipcode.getOrElse(TaxLineItemType.noZipcode)) ++
      Seq(TotalLineItemType)
  }

  protected def itemsWithSummaries(items: LineItems): LineItems = {
    val nonSummaries = items.filter (item => item.nature != LineItemNature.Summary )

    val subtotal = items.find( item => item.codeType == CodeType.Subtotal )
      .getOrElse(SubtotalLineItemType.lineItems(nonSummaries, Seq()).head)

    val total = items.find( item => item.codeType == CodeType.Total )
      .getOrElse(TotalLineItemType.lineItems(nonSummaries, Seq()).head)

    subtotal +: (total +: nonSummaries)
  }
}



//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Store must:
*  -give persistence to UnpersistedCheckout
*  -give update (of updated field) to PersistedCheckout
*  -give helper queries for getting checkouts
*    -needs schema, lineItemStore, and lineItemTypeStore
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

class CheckoutStore @Inject() (
  schema: Schema,
  lineItemStore: LineItemStore
) extends InsertsAndUpdates[CheckoutEntity] with SavesCreatedUpdated[CheckoutEntity] {

  override protected def table = schema.checkouts

  override def withCreatedUpdated(toUpdate: CheckoutEntity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }

  // only Unpersisted can save itself (create a checkout)
  // TODO(SER-499): re-evaluate this, see about moving saving into checkout
  def create(checkout: Checkout): Checkout = {
    val savedEntity = insert(checkout._entity)
    val savedItems  =
      for (item <- checkout.lineItems) yield {
        item.withCheckoutId(savedEntity.id).transact(checkout.id)
      }

    Checkout( savedEntity, savedItems )
  }

  // only updates updated column of checkout
  def update(checkout: Checkout): Checkout = {
    checkout.copy(
      _entity = update(checkout._entity)
    )
  }

  // helper queries
  def findById(id: Long): Option[Checkout] = {
    // val entity = //table.where()
    // val lineItems = lineItemStore.getItemsByCheckoutId(id)
    None
  }

  def getCheckoutsByCustomerId(id: Long): Seq[Checkout] = {
    Nil
  }
}

//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


