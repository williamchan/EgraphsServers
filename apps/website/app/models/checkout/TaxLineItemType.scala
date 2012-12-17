package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}
import scalaz.Lens

case class TaxLineItemType (
  _entity: LineItemTypeEntity,
  zipCode: String // or Int?
) extends LineItemType[Money] with HasLineItemTypeEntity
  with LineItemTypeEntityLenses[TaxLineItemType]
  with LineItemTypeEntityGetters[TaxLineItemType]
  with LineItemTypeEntitySetters[TaxLineItemType]
{

  override val toJson = {
    // TODO(SER-499): implement
    ""
  }

  /**
   * Calculates tax as applied to the value of the subtotal and discounts.
   * @param resolvedItems
   * @param pendingResolution
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    def isDiscountType(itemType: LineItemType[_]) = itemType.nature == LineItemNature.Discount
    def isSubtotalItem(item: LineItem[_]) = item.itemType.codeType == CodeType.Subtotal

    (resolvedItems.find(isSubtotalItem(_)), pendingResolution.find(isDiscountType(_))) match {
      // Want to have the subtotal and no pending discounts
      case (Some(subtotal: SubtotalLineItem), None) =>

        // Sum discounts since there may be multiple (eventually)
        val totalDiscount = resolvedItems.foldLeft(Money.zero(CurrencyUnit.USD)) { (acc, next) =>
          if (isDiscountType(next.itemType)) acc plus next.amount else acc
        }

        val subtotalLessDiscounts = subtotal.amount minus totalDiscount
        val totalTax = subtotalLessDiscounts multipliedBy (taxRates.sum.bigDecimal, java.math.RoundingMode.UP)

        Seq(TaxLineItem(this, totalTax))

      case _ => Nil
    }
  }


  override protected lazy val entityLens = Lens[TaxLineItemType, LineItemTypeEntity](
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )

  // TODO: add more cases as needed, verify WA tax rate
  protected val taxByZip: Map[String, BigDecimal] = Map(
    /** Washington zipcodes: 980XX - 994XX; State-wide sales tax of 6.5% */
    "9[8-9][0-4][0-9][0-9]" -> BigDecimal(0.065)
  )

  /** Get applicable taxes based on zip */
  protected def taxRates: Seq[BigDecimal] = {
    taxByZip.filter(zipCode matches _._1).values.toSeq
  }
}

object TaxLineItemType {
  def apply(zip: String) = {
    new TaxLineItemType(
      new LineItemTypeEntity(description, nature, codeType),
      zip
    )
  }

  val description = "Tax"
  val nature = LineItemNature.Tax
  val codeType = CodeType.Tax
}