package models.checkout

import models.enums._
import org.joda.money.{CurrencyUnit, Money}


package object checkout {
  /**
   * Pimps some helpers on to `Seq`s of LineItem`s and `LineItemType`s and provides some
   * type aliases for convenience
   */
  object Conversions {
    type LineItems = Seq[LineItem[_]]
    type LineItemTypes = Seq[LineItemType[_]]
    type FailureOrCheckout = Either[Checkout.CheckoutFailed, Checkout]

    //
    // LineItems and LineItemTypes DSL conversion
    //
    implicit def itemTypeSeqToMemberDSL(types: LineItemTypes) = new HasNatureAndCodeTypeToMemberDSL(types) {

      def apply(codeType: CheckoutCodeType): LineItemTypes = ofCodeType(codeType)

      def ofCodeType(codeType: CheckoutCodeType): LineItemTypes = types.filter(_.codeType == codeType)
    }


    implicit def lineItemSeqToMemberDSL(items: LineItems) = new HasNatureAndCodeTypeToMemberDSL(items) {

      def apply[LIT <: LineItemType[_], LI <: LineItem[_]](codeType: OfCheckoutClass[LIT, LI])
      : Seq[LI] = { ofCodeType(codeType) }

      def ofCodeType[LIT <: LineItemType[_], LI <: LineItem[_]] (codeType: OfCheckoutClass[LIT, LI])
      : Seq[LI] = { items.flatMap(_.asCodeTypeOption(codeType)) }

      def sumAmounts: Money = items.foldLeft(Money.zero(CurrencyUnit.USD))( _ plus _.amount )
    }


    abstract class HasNatureAndCodeTypeToMemberDSL[T <: HasLineItemNature with HasCodeType](elements: Seq[T]) {
      def apply(nature: LineItemNature) = ofNature(nature)

      def ofNature(nature: LineItemNature): Seq[T] = elements.filter(_.nature == nature)
      def ofNatures(natures: LineItemNature*): Seq[T] = elements.filter(natures contains _.nature)
      def notOfNature(nature: LineItemNature): Seq[T] = elements.filterNot(_.nature == nature)
      def notOfNatures(natures: LineItemNature*): Seq[T] = elements.filterNot(natures contains _.nature)

      def ofCodeTypes(codeTypes: CheckoutCodeType*): Seq[T] = elements.filter(codeTypes contains _.codeType)
      def notOfCodeType(codeType: CheckoutCodeType): Seq[T] = elements.filterNot(_.codeType == codeType)
      def notofCodeTypes(codeTypes: CheckoutCodeType*): Seq[T] = elements.filterNot(codeTypes contains _.codeType)
    }
  }
}
