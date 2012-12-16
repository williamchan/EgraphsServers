package models.checkout

import models.Coupon
import models.enums.{CodeType, LineItemNature, CouponType, CouponDiscountType, CouponUsageType}
import org.joda.money.Money
import services.AppConfig
import services.db.Schema
import scalaz.Lens
import org.squeryl.PrimitiveTypeMode._

/**
 * Services for gift certificates and associated [[models.checkout.GiftCertificateLineItemType]]s items. Use like this:
 * {{{
 *   import models.checkout.GiftCertificateLineItemTypeComponent
 *
 *   trait MyEndpoint { this: Controller with GiftCertificateLineItemTypeComponent =>
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
protected object GiftCertificateComponent extends LineItemComponent {
  /* NOTE(SER-499): this could be broke out for arbitrary injection with an abstract class
   * {{{
   *   abstract class GiftCertificateComponent(protected val schema: Schema) extends // ...
   *   // ...
   *   object GiftCertificateHelper extends GiftCertificateComponent(mySchema)
   *   import GiftCertificateHelper.GiftCertificateLineItemTypeServices.Conversions._
   * }}}
   */
  protected val schema: Schema = AppConfig.instance[Schema]
}

object GiftCertificateLineItemServices
  extends GiftCertificateComponent.SavesAsLineItemEntity[GiftCertificateLineItem]
{

  object Conversions extends EntitySavingConversions {
    // pimp things out in here
    implicit def itemToSavingDsl(lineItem: GiftCertificateLineItem): SavingDSL = {
      new SavingDSL(lineItem, lineItem._entity)
    }
  }

  def modelWithNewEntity(certificateItem: GiftCertificateLineItem, entity: LineItemEntity) = {
    certificateItem.copy(_entity = entity)
  }
}

object GiftCertificateLineItemTypeServices
  extends GiftCertificateComponent.SavesAsLineItemTypeEntity[GiftCertificateLineItemType]
{

  override protected def modelWithNewEntity(certificateType: GiftCertificateLineItemType, entity: LineItemTypeEntity) = {
    certificateType.copy(_entity=entity)
  }

  /**
   * Query DSL that pimps the companion object with DB querying functionality,
   * e.g. `GiftCertificateLineItemType.getWithAmount(Money.parse("$50")`, and model
   * instances with saving functionality.
   */
  object Conversions extends EntitySavingConversions {
    implicit def typeToSavingDsl(lineItemType: GiftCertificateLineItemType): SavingDSL = {
      new SavingDSL(lineItemType, lineItemType._entity)
    }

    implicit def companionToQueryDsl(companion: GiftCertificateLineItemType.type) = {
      QueryDSL
    }

    object QueryDSL {
      private lazy val seedEntity = GiftCertificateLineItemType.entityWithDescription()

      /** @return unpersisted Gift Certificate for given amount to given recipient */
      def getWithRecipientAndAmount(recipient: String, amount: Money): GiftCertificateLineItemType = {
        GiftCertificateLineItemType(recipient, amount)
      }
    }
  }
}


////////////////////////////////////////////////////////////////////////////////////////////////////

case class GiftCertificateLineItemType (
  _entity: LineItemTypeEntity,
  recipient: String,
  amountToBuy: Money

) extends LineItemType[Coupon] with HasLineItemTypeEntity
  with LineItemTypeEntityLenses[GiftCertificateLineItemType]
  with LineItemTypeEntityGetters[GiftCertificateLineItemType]
{
  // TODO(SER-499): implement
  override def toJson: String = ""
  override def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    Nil
  }

  //
  // LineItemTypeEntityLenses members
  //
  override protected lazy val entityLens = GiftCertificateLineItemType.entityLens
}

// / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /

object GiftCertificateLineItemType {
  // for convenience
  val nature = LineItemNature.Discount
  val codeType = CodeType.GiftCertificate

  val entityLens = Lens[GiftCertificateLineItemType, LineItemTypeEntity] (
    get = cert => cert._entity,
    set = (cert, entity) => cert.copy(entity)
  )

  def apply(recipient: String, amountToBuy: Money): GiftCertificateLineItemType = {
    new GiftCertificateLineItemType(
      entityWithDescription(description(recipient, amountToBuy)),
      recipient,
      amountToBuy
    )
  }

  def description(recip: String, amount: Money) = "Gift certificate for " + amount + " to " + recip
  def entityWithDescription(desc: String = "Gift certificate") =
    new LineItemTypeEntity(0, desc, nature, codeType)
}

////////////////////////////////////////////////////////////////////////////////////////////////////


/**
 *
 * @param _entity
 * @param itemType - line item type corresponding to this gift certificate
 * @param amount
 * @param subItems - items that depend upon or relate strongly to this gift certificate
 * @param checkoutId
 * @param _domainEntityId
 */
case class GiftCertificateLineItem (
  _entity: LineItemEntity = new LineItemEntity(),
  itemType: GiftCertificateLineItemType,
  amount: Money,
  subItems: Seq[LineItem[_]] = Nil,
  checkoutId: Long = 0,
  _domainEntityId: Long = 0
) extends LineItem[Coupon] with HasLineItemEntity {

  // TODO(ser-499): implement once api nailed down
  override def toJson: String = ""

  override def domainObject: Coupon = {
    if (_domainEntityId > 0) {
      // TODO(SER-499): query db for coupon
      null // to be a query

    } else {
      new Coupon(name = couponName, discountAmount = amount.getAmount)
        .withCouponType(CouponType.GiftCertificate)
        .withDiscountType(CouponDiscountType.Flat)
        .withUsageType(CouponUsageType.Prepaid)
    }
  }

  // TODO(SER-499): this may be better if implemented with lenses
  def withTypeAndCheckoutId(newType: GiftCertificateLineItemType, newId: Long) = this.copy(
    _entity = _entity.copy(_checkoutId = newId),
    checkoutId = newId,
    itemType = newType
  )


  // TODO(SER-499): needs LineItemComponent
  override def transact(newCheckoutId: Long = 0): GiftCertificateLineItem = {
    import GiftCertificateLineItemServices.Conversions._
    import GiftCertificateLineItemTypeServices.Conversions._

    // save line item type entity
    val savedType: GiftCertificateLineItemType = null

    // save this entity
    val itemWithSavedEntity = withTypeAndCheckoutId(savedType, newCheckoutId).create()

    // save this domain entity
    val savedCoupon = itemWithSavedEntity.domainObject.save()

    // return new item with its new type
    GiftCertificateLineItem(itemWithSavedEntity._entity, savedType, amount, subItems, newCheckoutId, savedCoupon.id)
  }

  //
  // Coupon helpers
  //
  protected val couponNameFormatString = "A gift certificate for %s"
  protected def couponName: String = {
    couponNameFormatString.format(itemType.recipient)
  }
}