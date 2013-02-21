package models.checkout

import scala.collection.GenTraversable
import org.joda.money.{CurrencyUnit, Money}
import models.enums._

//TODO: Shouldn't this be up a level, so you don't have to import it from within
// the package?
package object checkout {
  /**
   * Pimps some helpers on to `Seq`s of LineItem`s and `LineItemType`s and provides some
   * type aliases for convenience
   */
  object Conversions {
    type LineItems = GenTraversable[LineItem[_]]
    type LineItemTypes = GenTraversable[LineItemType[_]]
    type FailureOrCheckout = Either[Checkout.CheckoutFailed, Checkout]

    //
    // LineItems and LineItemTypes DSL conversion
    //
    implicit def itemTypeSeqToMemberDSL(types: LineItemTypes) = new HasNatureAndCodeTypeToMemberDSL(types) {
      /** applies `ofCodeType` */
      def apply(codeType: CheckoutCodeType): LineItemTypes = ofCodeType(codeType)

      /** filters `types` by codeType */
      def ofCodeType(codeType: CheckoutCodeType): LineItemTypes = types.filter(_.codeType == codeType)
    }

    implicit def lineItemSeqToMemberDSL(items: LineItems) = new HasNatureAndCodeTypeToMemberDSL(items) {
      /** applies `ofCodeType` */
      def apply[LIT <: LineItemType[_], LI <: LineItem[_]](codeType: CheckoutCodeType with OfCheckoutClass[LIT, LI])
      : Seq[LI] = { ofCodeType(codeType) }

      /** filters `items` by CodeType to a Seq[LI] where LI is the LineItem implementation */
      def ofCodeType[LIT <: LineItemType[_], LI <: LineItem[_]] (codeType: CheckoutCodeType with OfCheckoutClass[LIT, LI])
      : Seq[LI] = { items.toSeq.seq.flatMap(_.asCodeTypeOption(codeType)) }

      /** adds amounts of the LineItems in `items` */
      def sumAmounts: Money = items.foldLeft(Money.zero(CurrencyUnit.USD))( _ plus _.amount )
    }


    abstract class HasNatureAndCodeTypeToMemberDSL[T <: HasLineItemNature with HasCodeType](elements: GenTraversable[T]) {
      def apply(nature: LineItemNature) = ofNature(nature)

      /** filters by Nature */
      def ofNature(nature: LineItemNature): GenTraversable[T] = elements.filter(_.nature == nature)
      def ofNatures(natures: LineItemNature*): GenTraversable[T] = elements.filter(natures contains _.nature)
      def notOfNature(nature: LineItemNature): GenTraversable[T] = elements.filterNot(_.nature == nature)
      def notOfNatures(natures: LineItemNature*): GenTraversable[T] = elements.filterNot(natures contains _.nature)

      /** filters by CodeType */
      def ofCodeTypes(codeTypes: CheckoutCodeType*): GenTraversable[T] = elements.filter(codeTypes contains _.codeType)
      def notOfCodeType(codeType: CheckoutCodeType): GenTraversable[T] = elements.filterNot(_.codeType == codeType)
      def notofCodeTypes(codeTypes: CheckoutCodeType*): GenTraversable[T] = elements.filterNot(codeTypes contains _.codeType)
    }
  }
}
