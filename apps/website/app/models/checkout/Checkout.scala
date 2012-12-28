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

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Want to reuse as much existing code as possible, but:
*  -Entity operations
*  -Persistence
*  -Updated/created
*
* Follow existing patterns:
*  -Stores
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
sealed trait Checkout {
  // entity for persistence
  def _entity: CheckoutEntity

  // uses LineItems and LineItemTypes
  def lineItems: LineItems
  def lineItemTypes: LineItemTypes

  // uses services, but subclasses should only access certain methods
  def services: CheckoutServices

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

case class CheckoutEntity(
  id: Long = 0,
  customerId: Long = 0,
  created: Timestamp,
  updated: Timestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = CheckoutEntity.unapply(this)
}


case class CheckoutServices @Inject() (
  checkoutStore: CheckoutStore
)


//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Created:
*  -can save, not update
*  -takes types, generates items
*  -needs zipcode to be created for tax purposes
*
* Restored:
*  -cannot save or update itself
*  -takes items, extracts types
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

/** For creating */
case class UnpersistedCheckout (
  _entity: CheckoutEntity,
  _lineItemTypes: LineItemTypes,
  maybeZipcode: Option[String],
  services: CheckoutServices
) extends Checkout {

  //
  // Checkout Members
  //
  /**
   * Appends taxes, fees, and summaries to _lineItemTypes
   * @return
   */
  override def lineItemTypes: LineItemTypes = {
    _lineItemTypes ++ taxesAndSummariesByZip(maybeZipcode)
  }

  /**
   * Generate lineItems from lineItemTypes
   * @return
   */
  override lazy val lineItems: LineItems = {
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

  //
  // UnpersistedCheckout methods
  //
  /**
   * Adds extra types to this checkout
   * @param additionalTypes
   * @return new UnpersistedCheckout with additional lineItemTypes
   */
  def add(additionalTypes: LineItemTypes): UnpersistedCheckout = {
    this.copy(_lineItemTypes = _lineItemTypes ++ additionalTypes)
  }

  /**
   * Persists this checkout, its types, and its items
   * @return PersistedCheckout of the transacted checkout
   */
  def transact(): PersistedCheckout = {
    // save _entity, lineItemTypes, and lineItems
    services.checkoutStore.create(this)
  }


  //
  // Helper methods
  //
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
}


/** After creating/for updating */
case class PersistedCheckout (
  _entity: CheckoutEntity,
  lineItems: LineItems,
  services: CheckoutServices = AppConfig.instance[CheckoutServices]
) extends Checkout {

  //
  // Checkout members
  //
  override def lineItemTypes: LineItemTypes = {
    // extract lineItemTypes from lineItems
    lineItems.map(item => item.itemType)
  }

  //
  // PersistedCheckout methods
  //
  def update(): PersistedCheckout = {
    // add new items, summaries, charges, etc to lineitems

    // update checkout entity's updated field
    services.checkoutStore.update(this)
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
  def create(checkout: UnpersistedCheckout): PersistedCheckout = {
    val savedEntity = insert(checkout._entity)
    val savedItems  =
      for (item <- checkout.lineItems) yield {
        item.withCheckoutId(savedEntity.id).transact(checkout.id)
      }

    new PersistedCheckout( savedEntity, savedItems )
  }

  // only updates updated column of checkout
  def update(checkout: PersistedCheckout): PersistedCheckout = {
    checkout.copy(
      _entity = update(checkout._entity)
    )
  }

  // helper queries
  def getCheckoutById(id: Long): Option[PersistedCheckout] = {
    // val entity = //table.where()
    // val lineItems = lineItemStore.getItemsByCheckoutId(id)
    None
  }

  def getCheckoutsByCustomerId(id: Long): Seq[PersistedCheckout] = {
    Nil
  }
}

//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


