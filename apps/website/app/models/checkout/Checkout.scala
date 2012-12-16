package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.LineItemNature

/**
 * @param lineItemTypes -- intermediate form of contents of checkout
 */
case class Checkout(lineItemTypes: Seq[LineItemType[_]] = Nil) {

  def add(additionalTypes: Seq[LineItemType[_]]): Checkout = {
    additionalTypes match {
      case Nil => this
      case _ => this.copy(lineItemTypes = lineItemTypes ++ additionalTypes)
    }
  }

  // TODO(SER-499): create and append subtotal, tax, total to this Seq
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

  // TODO(SER-499): use SubtotalLineItem
  /** @return sum of all products in checkout */
  private def calculateSubtotal: Money = {
    // TODO(SER-499): this is super awkward, would be nice to refactor
    def isProduct(item: LineItem[_]) = item.itemType._entity._nature == LineItemNature.Product.name

    lineItems.foldLeft (Money.zero(CurrencyUnit.USD)) {
      (acc: Money, nextItem: LineItem[_]) =>
        if (isProduct(nextItem)) acc plus nextItem.amount else acc
    }
  }

  // TODO(SER-499): tax and total, should grab from lineItems
  // lazy val tax: TaxLineItem
  // lazy val total: TotalLineItem

  // private def calculateTax
  // private def calculateTotal

  // def lineItemsToJson: String
  // def lineItemTypesToJson: String

  def transact: Checkout = {
    // transact checkout entity
    // for(lineItem <- lineItems) yield lineItem.withCheckoutId(id).transact
    this
  }
}

object Checkout {
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