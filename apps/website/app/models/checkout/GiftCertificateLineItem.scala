package models.checkout

import models.Coupon
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


case class GiftCertificateLineItemType (_entity: LineItemTypeEntity, amountToBuy: Money)
  extends LineItemType[Coupon]
  with LineItemTypeEntityLenses[GiftCertificateLineItemType]
  with LineItemTypeEntityGetters[GiftCertificateLineItemType]
{

  def lineItems(resolvedItems: IndexedSeq[LineItem[_]], pendingResolution: IndexedSeq[LineItemType[_]]) = {
    None
  }

  //
  // LineItemTypeEntityLenses members
  //
  override protected lazy val entityLens = GiftCertificateLineItemType.entityLens
}


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
