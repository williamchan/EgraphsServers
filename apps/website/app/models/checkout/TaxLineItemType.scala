package models.checkout

import org.joda.money.{CurrencyUnit, Money}
import models.enums.{CodeType, LineItemNature}
import java.sql.Timestamp
import scalaz.Lens
import services.db.{Schema, CanInsertAndUpdateAsThroughServices}
import com.google.inject.Inject
import services.{AppConfig, Time}
import play.api.libs.json.Json

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
        Seq(TaxLineItem(this, taxAmount))

      case _ => Nil
    }
  }


  override protected lazy val entityLens = Lens[TaxLineItemType, LineItemTypeEntity](
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )
}



object TaxLineItemType {
  val basicTaxName = "Tax"
  val nature = LineItemNature.Tax
  val codeType = CodeType.Tax
  val noZipcode = ""

  /**
   * Makes a single TaxLineItemType
   * @param taxRate
   * @param maybeTaxName - should describe the type of tax (ex: sales tax)
   * @return TaxLineItemType of given rate with given description
   */
  def apply(zipcode: String, taxRate: BigDecimal, maybeTaxName: Option[String]): TaxLineItemType = {
    val taxName = maybeTaxName.getOrElse(basicTaxName)
    TaxLineItemType(
      LineItemTypeEntity(
          entityDescription(zipcode, taxRate, taxName),
          nature,
          codeType
        ),
      taxRate
    )
  }

  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity): TaxLineItemType = {
    def isValid(rate: BigDecimal) = 0.0 <= rate && rate <= 1.0
    def taxRate = taxRateOptionFromEntity(entity)
      .getOrElse(throw new IllegalArgumentException("Tax rate could not be parsed from entity."))

    require(isValid(taxRate), "Invalid rate parsed from entity.")
    TaxLineItemType(entity, taxRate)
  }


  /**
   * Here we are only concerned with possible taxes; the ones that apply or not by product
   * (ex: digital product taxes vs. general sales tax) are determined in lineItems method.
   *
   * @param zipcode of customer
   * @return Sequence of TaxLineItemTypes for taxes that apply in the given zipcode are
   */
  def getTaxesByZip(zipcode: String): Seq[TaxLineItemType] = {
    for ((pattern, (rate, name)) <- zipToTaxMap if zipcode matches pattern) yield {
      val maybeTaxName = if (name.isEmpty) None else Some(name)
      TaxLineItemType(zipcode, rate, maybeTaxName)
    }
  }.toSeq


  /** Map of zipcode pattern to tuple of tax rate and description */
  protected val zipToTaxMap: Map[String, (BigDecimal, String)] = Map(
    TaxLineItemType.noZipcode -> (BigDecimal(0), ""),

    /** Washington zipcodes: 980XX - 994XX; State-wide sales tax of 6.5% */
    "9[8-9][0-4][0-9][0-9]" -> (BigDecimal(0.065), "WA Sales Tax")
  )


  protected val jsonZipKey = "Zipcode"
  protected val jsonRateKey = "TaxRate"
  protected val jsonNameKey = "TaxName"

  def zipcodeOptionFromEntity(entity: LineItemTypeEntity): Option[String] = {
    (Json.parse(entity._desc) \ jsonZipKey).asOpt[String]
  }

  def taxNameOptionFromEntity(entity: LineItemTypeEntity): Option[String] = {
    (Json.parse(entity._desc) \ jsonNameKey).asOpt[String]
  }

  def taxRateOptionFromEntity(entity: LineItemTypeEntity): Option[BigDecimal] = {
    (Json.parse(entity._desc) \ jsonRateKey).asOpt[Double].map(BigDecimal(_))
  }

  protected def entityDescription(zipcode: String, taxRate: BigDecimal, taxName: String): String = {
    Json.stringify {
      Json.toJson( Map (
        jsonZipKey -> Json.toJson(zipcode),
        jsonNameKey -> Json.toJson(taxName),
        jsonRateKey -> Json.toJson(taxRate.doubleValue)
      ))
    }
  }
}








case class TaxLineItemTypeServices @Inject() (schema: Schema)
  extends SavesAsLineItemTypeEntity[TaxLineItemType]
{
  override protected def modelWithNewEntity(tax: TaxLineItemType, entity: LineItemTypeEntity) = {
    tax.copy(_entity=entity)
  }
}
