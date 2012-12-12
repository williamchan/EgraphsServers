package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.Money
import services.db.Schema
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._

/**
 * Services for gift certificates and associated [[models.checkout.GiftCertificateLineItemType]]s items. Use like this:
 * {{{
 *   import models.checkout.GiftCertificateComponent
 *
 *   trait MyEndpoint { this: Controller with GiftCertificateComponent =>
 *     // import the implicit conversions that enhance the otherwise plain
 *     // gift certificate classes
 *     import GiftCertificateServices.Conversions._
 *
 *      def doSomething = Action {
 *        // Get the LineItemType from the store
 *        val lineItemType = GiftCertificateLineItemType.getWithRecipientAndAmount("Joe", Money.parse("$50"))
 *
 *        // Save it if you want
 *        lineItemType.update()
 *
 *         Ok
 *      }
 *   }
 * }}}
 */
trait GiftCertificateComponent { this: LineItemTypeEntityComponent =>
  protected def schema: Schema

  object GiftCertificateServices extends SavesAsLineItemTypeEntity[GiftCertificateLineItemType]
  {
    /**
     * Query DSL that pimps the companion object with DB querying functionality,
     * e.g. `GiftCertificateLineItemType.getWithAmount(Money.parse("$50")`, and model
     * instances with saving functionality.
     */
    object Conversions extends EntitySavingConversions {

      implicit def companionToQueryDsl(companion: GiftCertificateLineItemType.type) = {
        QueryDSL
      }

      object QueryDSL {
        private lazy val seedEntity = GiftCertificateLineItemType.defaultEntity

        // TODO(SER-499): this is the complete opposite of how entities should be used for gift cert's
        def getWithRecipientAndAmount(recipient: String, amount: Money): GiftCertificateLineItemType = {
          val entity = table.where(_.codeType.name === seedEntity.codeType.name).headOption.getOrElse {
            table.insert(seedEntity)
          }
          GiftCertificateLineItemType(entity, recipient, amount)
        }
      }
    }

    //
    // SavesAsLineItemTypeEntity members
    //
    override protected def modelWithNewEntity(certificate: GiftCertificateLineItemType, entity: LineItemTypeEntity) = {
      certificate.copy(_entity=entity)
    }
  }
}


////////////////////////////////////////////////////////////////////////////////////////////////////

case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String,
  amountToBuy: Money

) extends LineItemType[Coupon]
  with LineItemTypeEntityLenses[GiftCertificateLineItemType]
  with LineItemTypeEntityGetters[GiftCertificateLineItemType]
{

  def lineItems(resolvedItems: IndexedSeq[LineItem[_]], pendingResolution: IndexedSeq[LineItemType[_]]) = {
    Some( IndexedSeq( new GiftCertificateLineItem( itemType = this )))
  }

  //
  // LineItemTypeEntityLenses members
  //
  override protected lazy val entityLens = GiftCertificateLineItemType.entityLens
}

// / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /

object GiftCertificateLineItemType {
  val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )

  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    GiftCertificateLineItemType(
      LineItemTypeEntity()
        .withNature(LineItemNature.Product)
        .withCodeType(CodeType.GiftCertificateLineItemType),
      recipient,
      amountToBuy
    )
  }

  lazy val defaultEntity = LineItemTypeEntity(_desc = "Gift Certificate")
    .withNature(LineItemNature.Product)
    .withCodeType(CodeType.GiftCertificateLineItemType)
}

////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Want to be able to:
 * -represent checkout/cart contents
 * -generate the model that we're representing
 * -...
 * -convenience methods?
 *
 *
 *
 * @param itemType - line item type corresponding to this gift certificate
 * @param subItems - items that depend upon or relate strongly to this gift certificate
 */
case class GiftCertificateLineItem (
  _entity: LineItemEntity = new LineItemEntity(),
  itemType: GiftCertificateLineItemType,
  maybeDescription: Option[String] = None,
  subItems: IndexedSeq[LineItem[_]] = IndexedSeq()

) extends LineItem[Coupon] {

  // TODO(SER-499): may want to pull this out into a conf or something
  protected val couponNameFormatString = "A gift certificate for %s"

  override def description = maybeDescription.getOrElse(itemType.description)
  override def amount = itemType.amountToBuy


  /**
   * Persist Gift Certificate as a Coupon
   * @return generated coupon
   */
  override def transact: Coupon = {
    // TODO(SER-499): probably want to perform some validation on this before saving
    new Coupon(
      name = couponName,
      discountAmount = amount.getAmount,
      _couponType = CouponType.GiftCertificate.name,
      _discountType = CouponDiscountType.Flat.name,
      _usageType = CouponUsageType.OneUse.name,
      isActive = true
    ).save()
  }


  /** helper for creating a coupon name */
  protected def couponName: String = {
    couponNameFormatString.format(itemType.recipient)
  }
}
