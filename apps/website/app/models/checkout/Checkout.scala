package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.LineItemNature
import org.squeryl.KeyedEntity
import services.db.HasEntity

package object checkout {
  /** For _domainEntityId if domain object doesn't get persisted; ex: Tax */
  val UnusedDomainEntity: Long = -1

  /** For id's of line items and line item types that don't get persisted; ex: Subtotal */
  val Unpersisted: Long = -2


  val UnsavedEntity = 0
}

/**
 *
 * @param _entity
 * @param lineItemTypes - intermediate form of contents of checkout
 */
case class Checkout private(
  _entity: CheckoutEntity,
  lineItemTypes: Seq[LineItemType[_]] = Nil,
  _lineItems: Seq[LineItem[_]] = Nil
) extends HasEntity[CheckoutEntity] {



  def add(additionalTypes: Seq[LineItemType[_]]): Checkout = {
    additionalTypes match {
      case Nil => this
      case _ => this.copy(lineItemTypes = lineItemTypes ++ additionalTypes)
    }
  }


  // TODO(SER-499): append subtotal, tax, total to this Seq; get _lineItems first
  lazy val lineItems: Seq[LineItem[_]] = {
    // TODO(SER-499): add subtotal, tax, fees, total before processing

    case class ResolutionPass(items: Seq[LineItem[_]], unresolved: Seq[LineItemType[_]]) {
      def isComplete: Boolean = unresolved.isEmpty
    }

    def executePass(passToResolve: ResolutionPass): Seq[LineItem[_]] = {
      if (passToResolve.isComplete) {
        passToResolve.items
      } else {
        val resolvedPass = passToResolve.unresolved.foldLeft(passToResolve) { (oldPass, nextItemType) =>
          val itemTypesSansCurrent = oldPass.unresolved.filter(_ != nextItemType)

          nextItemType.lineItems(oldPass.items, itemTypesSansCurrent) match {
            case Nil => oldPass
            case newLineItems: Seq[LineItem[_]] => oldPass.copy(newLineItems, itemTypesSansCurrent)

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


  lazy val flattenedLineItems: Seq[LineItem[_]] = {
    for (lineItem <- lineItems; flattened <- lineItem.flatten) yield flattened
  }


  // TODO(SER-499): make helpers for getting by nature, codeType


  def transact(): Checkout = {
    // TODO(SER-499): implement this
    // transact checkout entity
    // for(lineItem <- lineItems) yield lineItem.withCheckoutId(id).transact
    if (id <= 0) {
      import CheckoutServices.Conversions._
      this.create().transact()
    } else {
      // _entity is persisted, so now persist line items

      // NOTE(SER-499): a fresh checkouts lineItems are directly derived from the lineItemTypes, but after transacting, lineItems and lineItemTypes could become out of sync; would be great to make it easy to know what state we're in (perhaps monadicly).
      val (savedLineItems, savedItemTypes) = (for(item <- flattenedLineItems) yield {
        val savedItem = item.withCheckoutId(id).transact()
        (savedItem, savedItem.itemType)
      }).unzip

      this.copy(lineItemTypes = savedItemTypes, _lineItems = savedLineItems)

    }

    this
  }


  // TODO(SER-499): checkout serialization
  // def lineItemsToJson: String
  // def lineItemTypesToJson: String


  // convenience methods
  protected def id = _entity.id
}

object Checkout {
  def apply(lineItemTypes: Seq[LineItemType[_]]): Checkout =
    Checkout(CheckoutEntity(), lineItemTypes)

  /**
   * @param json -- serialized LineItemTypes
   * @return Option of restored Checkout if deserializing succeeds; otherwise, None.
   */
  def restore(json: String): Option[Checkout] = {
    None
  }

  def getWithId(id: Long): Option[Checkout]= {
    None
  }
}




case class CheckoutEntity(
  id: Long = 0,
  customerId: Long = 0
) extends KeyedEntity[Long]


object CheckoutServices extends LineItemComponent.SavesAsCheckoutEntity {

  object Conversions extends CheckoutSavingConversions

  def modelWithNewEntity(checkout: Checkout, entity: CheckoutEntity) = {
    checkout.copy(_entity = entity)
  }
}