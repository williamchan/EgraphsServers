package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}
import java.sql.Timestamp
import scalaz.Lens
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject
import services.{AppConfig, Time}

case class TaxLineItemType private (
  _entity: LineItemTypeEntity,
  rate: BigDecimal,
  services: TaxLineItemTypeServices = AppConfig.instance[TaxLineItemTypeServices]
) extends LineItemType[Money] with HasLineItemTypeEntity  // TODO(SER-499): is this necessary?
  with LineItemTypeEntityGettersAndSetters[TaxLineItemType]
  with CanInsertAndUpdateAsThroughServices[TaxLineItemType, LineItemTypeEntity]
{

  override def toJson: String = {
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

    val maybeResolvedSubtotal = resolvedItems.find(isSubtotalItem(_))
    val maybePendingDiscount = pendingResolution.find(isDiscountType(_))

    (maybeResolvedSubtotal, maybePendingDiscount) match {
      // Want to have the subtotal and no pending discounts
      case (Some(subtotal: SubtotalLineItem), None) =>

        // Sum discounts since there may be multiple (eventually)
        val totalDiscount = resolvedItems.foldLeft(Money.zero(CurrencyUnit.USD)) { (acc, next) =>
          if (isDiscountType(next.itemType)) acc plus next.amount else acc
        }

        // (subtotal - discounts) * tax rate
        val taxAmount = (subtotal.amount minus totalDiscount) multipliedBy (rate.bigDecimal, java.math.RoundingMode.UP)
        Seq(TaxLineItem(this, taxAmount, None))

      case _ => Nil
    }
  }


  override protected lazy val entityLens = Lens[TaxLineItemType, LineItemTypeEntity](
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )
}



object TaxLineItemType {
  val basicDescription = "Tax"
  val nature = LineItemNature.Tax
  val codeType = CodeType.Tax
  val noZipcode = ""

  /**
   * Makes a single TaxLineItemType
   * @param taxRate
   * @param maybeDescription - should describe the type of tax (ex: sales tax)
   * @return TaxLineItemType of given rate with given description
   */
  def apply(taxRate: BigDecimal, maybeDescription: Option[String]): TaxLineItemType = {
    TaxLineItemType(
      LineItemTypeEntity(
        maybeDescription.getOrElse(basicDescription),
        nature,
        codeType),
      taxRate
    )
  }

  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity): TaxLineItemType = {
    def rate = BigDecimal(itemEntity.notes.stripSuffix("%").toDouble/100)
    def isValidRate = 0.0 <= rate && rate <= 1.0
    require(isValidRate, "Invalid rate parsed from entity.")

    TaxLineItemType(entity, rate)
  }


  /**
   * Here we are only concerned with possible taxes; the ones that apply or not by product
   * (ex: digital product taxes vs. general sales tax) are determined in lineItems method.
   *
   * @param zipcode of customer
   * @return Sequence of TaxLineItemTypes for taxes that apply in the given zipcode are
   */
  def getTaxesByZip(zipcode: String): Seq[TaxLineItemType] = {
    for ((pattern, (rate, desc)) <- zipToTaxMap if zipcode matches pattern) yield {
      val description = if (desc isEmpty) basicDescription else desc
      TaxLineItemType(rate, Some(description))
    }
  }.toSeq


  /** Map of zipcode pattern to tuple of tax rate and description */
  protected val zipToTaxMap: Map[String, (BigDecimal, String)] = Map(
    TaxLineItemType.noZipcode -> (BigDecimal(0), ""),

    /** Washington zipcodes: 980XX - 994XX; State-wide sales tax of 6.5% */
    "9[8-9][0-4][0-9][0-9]" -> (BigDecimal(0.065), "WA Sales Tax")
  )
}








case class TaxLineItemTypeServices @Inject() (schema: Schema)
  extends SavesAsLineItemTypeEntity[TaxLineItemType]
{
  override protected def modelWithNewEntity(tax: TaxLineItemType, entity: LineItemTypeEntity) = {
    tax.copy(_entity=entity)
  }
}
