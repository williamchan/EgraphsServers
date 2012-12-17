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
object GiftCertificateLineItemServices
  extends LineItemComponent.SavesAsLineItemEntity[GiftCertificateLineItem]
{

  object Conversions extends LineItemSavingConversions

  def modelWithNewEntity(certificateItem: GiftCertificateLineItem, entity: LineItemEntity) = {
    certificateItem.copy(_entity = entity)
  }
}

object GiftCertificateLineItemTypeServices
  extends LineItemComponent.SavesAsLineItemTypeEntity[GiftCertificateLineItemType]
{

  override protected def modelWithNewEntity(certificateType: GiftCertificateLineItemType, entity: LineItemTypeEntity) = {
    certificateType.copy(_entity=entity)
  }

  /**
   * Query DSL that pimps the companion object with DB querying functionality,
   * e.g. `GiftCertificateLineItemType.getWithAmount(Money.parse("$50")`, and model
   * instances with saving functionality.
   */
  object Conversions extends LineItemTypeSavingConversions {

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
