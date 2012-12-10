package models.checkout

import models.Coupon
import models.enums.{CouponType, CouponDiscountType, CouponUsageType}
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
 *        val lineItemType = GiftCertificateLineItemType.getWithAmount(Money.parse("$50"))
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
        private val seedEntity = {
          LineItemTypeEntity(
            _desc = "Gift Certificate",
            codeType = "GiftCertificateLineItemType"
          ).withNature(LineItemNature.Product)
        }

        def getWithAmount(amount: Money): GiftCertificateLineItemType = {
          val entity = table.where(_.codeType === seedEntity.codeType).headOption.getOrElse {
            table.insert(seedEntity)
          }
          GiftCertificateLineItemType(entity, amount)
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

case class GiftCertificateLineItemType (_entity: LineItemTypeEntity, amountToBuy: Money)
  extends LineItemType[Coupon]
  with LineItemTypeEntityLenses[GiftCertificateLineItemType]
  with LineItemTypeEntityGetters[GiftCertificateLineItemType]
{

  def lineItems(resolvedItems: IndexedSeq[LineItem[_]], pendingResolution: IndexedSeq[LineItemType[_]]) = {

    // TODO(SER-499): add description, perhaps use some helpers vs. constructors?
    Some( IndexedSeq( new GiftCertificateLineItem(
      // TODO(SER-499): what should notes be?
      _entity = new LineItemEntity(_amountInCurrency = amountToBuy.getAmount(), notes = ""),
      itemType = this
    )))

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

  def apply(amountToBuy: Money): GiftCertificateLineItemType = {
    GiftCertificateLineItemType(
      LineItemTypeEntity().withNature(LineItemNature.Product),
      amountToBuy
    )
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Want to be able to:
 * -represent checkout/cart contents
 * -generate the model that we're representing
 * -...
 * -convenience methods?
 *
 * TODO(SER-499): tend to constructor parameter uncertainties
 *
 *
 * @param _entity - representation of database entry in LineItems table
 * @param itemType - line item type corresponding to this gift certificate
 * @param subItems - items that depend upon or relate strongly to this gift certificate
 */
case class GiftCertificateLineItem (
  _entity: LineItemEntity,                          // would a default make sense for this?
  itemType: GiftCertificateLineItemType,            // should this be a LineItemType instead?
  subItems: IndexedSeq[LineItem[_]] = IndexedSeq()  // should these be generated or set?

) extends LineItem[Coupon] {

  override def description = itemType.description
  override def amount = itemType.amountToBuy


  /**
   * TODO(SER-499): should we strip subItems from flattened items? I think convention says no.
   *
   * @return flat sequence of this gift certificate and its sub items, with the sub items of
   *         each item remaining in place.
   */
  override def flatten: IndexedSeq[LineItem[_]] = {
    val flatSubItemSeqSeq = for(subItem <- subItems) yield subItem.flatten
    IndexedSeq(this) ++ flatSubItemSeqSeq.flatten
  }


  /**
   * Persist Gift Certificate as a Coupon
   * @return generated coupon
   */
  override def transact: Coupon = {
    // TODO(SER-499): probably want to perform some validation on this before saving
    new Coupon(
      name = toName,
      discountAmount = amount.getAmount,
      _couponType = CouponType.GiftCertificate.name,
      _discountType = CouponDiscountType.Flat.name,
      _usageType = CouponUsageType.OneUse.name,
      isActive = true
    ).save()
  }

  /** helper for creating a coupon name */
  protected def toName: String = {
    // TODO(SER-499): probably want to pull this out into a conf or something
    "Gift certificate for the amount of " + amount
  }
}
