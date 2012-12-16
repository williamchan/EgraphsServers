package models.checkout

import services.db.Schema
import services.AppConfig
import org.joda.money.Money

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
