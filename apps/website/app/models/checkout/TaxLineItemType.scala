package models.checkout

import Conversions._
import com.google.inject.Inject
import models.enums.{CheckoutCodeType, LineItemNature}
import org.joda.money.{CurrencyUnit, Money}
import play.api.libs.json.Json
import scalaz.Lens
import services.db.Schema
import services.AppConfig


// TODO(SER-499): this stuff is functional, but has no tests. Will finish when done with Checkout Explorations, or possibly during the later stages of it.


/*
TODO: Taxes can be fleshed out further with a more flexible approach to getting rates, updating rates, etc.
-Keeping the rate from point of transaction in type entity can help with Checkout updates when occuring after a change in tax rates.
-Need to be able to get existing tax types by zipcode (currently creates new tax type based on rate pulled from hardcoded map
 */

case class TaxLineItemType protected (
  _entity: LineItemTypeEntity,
  zipcode: String,
  taxName: String,
  taxRate: BigDecimal,
  @transient _services: TaxLineItemTypeServices = AppConfig.instance[TaxLineItemTypeServices]
)
	extends LineItemType[Money]
	with HasLineItemTypeEntity[TaxLineItemType]
  with LineItemTypeEntityGettersAndSetters[TaxLineItemType]
  with SavesAsLineItemTypeEntityThroughServices[TaxLineItemType, TaxLineItemTypeServices]
{

  /**
   * Calculates tax as applied to the value of the subtotal and discounts.
   * @param resolvedItems
   * @param pendingResolution
   * @return Seq(new line items) if the line item type was successfully applied.
   *         Otherwise None, to signal that the checkout will try to resolve it again on the next round.
   */
  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes): Option[Seq[TaxLineItem]] = {
    import LineItemNature._

    (resolvedItems(CheckoutCodeType.Subtotal).headOption, pendingResolution(Discount)) match {
      // Want to have the subtotal and no pending discounts
      case (Some(subtotal: SubtotalLineItem), Nil) => Some {
        val totalDiscount = resolvedItems(Discount).sumAmounts

        // (subtotal - discounts) * tax rate
        val taxAmount = (subtotal.amount minus totalDiscount) multipliedBy (taxRate.bigDecimal, java.math.RoundingMode.UP)
        Seq(TaxLineItem(this, taxAmount))
      }

      case _ => None
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
  val codeType = CheckoutCodeType.Tax
  val noZipcode = ""

  /**
   * Makes a single TaxLineItemType
   * @param taxRate
   * @param maybeTaxName - should describe the type of tax (ex: sales tax)
   * @return TaxLineItemType of given rate with given description
   */
  def apply(zipcode: String, taxRate: BigDecimal, maybeTaxName: Option[String]): TaxLineItemType = {
    val taxName = maybeTaxName.getOrElse(basicTaxName)
    val desc = entityDescriptionAsJsonString(zipcode, taxRate, taxName)
    val entity = LineItemTypeEntity(desc, nature, codeType)

    new TaxLineItemType(entity, zipcode, taxName, taxRate)
  }

  def apply(entity: LineItemTypeEntity, itemEntity: LineItemEntity): TaxLineItemType = {
    def isValid(rate: BigDecimal) = 0.0 <= rate && rate <= 1.0
    def zipcode = zipcodeOptionFromEntity(entity)
      .getOrElse(throw new IllegalArgumentException("Zipcode could not be parsed from entity."))
    def taxName = taxNameOptionFromEntity(entity)
      .getOrElse(throw new IllegalArgumentException("Tax name could not be parsed from entity."))
    def taxRate = taxRateOptionFromEntity(entity)
      .getOrElse(throw new IllegalArgumentException("Tax rate could not be parsed from entity."))

    require(isValid(taxRate), "Invalid rate parsed from entity.")
    new TaxLineItemType(entity, zipcode, taxName, taxRate)
  }


  //
  // TODO: Need an easy way to get existing tax types by zip
  // Currently thinking it may have to suffice to store zip in description (using Json) and
  // search on that
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
    noZipcode -> (BigDecimal(0), ""),

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

  protected def entityDescriptionAsJsonString(zipcode: String, taxRate: BigDecimal, taxName: String): String = {
    Json.stringify {
      Json.toJson( Map (
        jsonZipKey -> Json.toJson(zipcode),
        jsonNameKey -> Json.toJson(taxName),
        jsonRateKey -> Json.toJson(taxRate.doubleValue)
      ))
    }
  }
}







// TODO(taxes): add helpers for getting taxes from a tax table when it is implemented
case class TaxLineItemTypeServices @Inject() (schema: Schema)
  extends SavesAsLineItemTypeEntity[TaxLineItemType]