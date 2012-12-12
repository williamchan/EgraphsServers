package models.checkout

import models.enums.{CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.Lens
import services.db.Schema
import services.payment.StripeCharge
import org.squeryl.PrimitiveTypeMode._





trait StripeChargeComponent { this: LineItemTypeEntityComponent =>
  protected def schema: Schema

  object StripeChargeServices extends SavesAsLineItemTypeEntity[StripeChargeLineItemType] {

    object Conversions extends EntitySavingConversions {

      implicit def companionToQueryDsl(companion: StripeChargeLineItemType.type) = {
        QueryDSL
      }

      object QueryDSL {
        private lazy val seedEntity = StripeChargeLineItemType.defaultEntity

        def getWithStripeIdAndAmount(id: String, amount: Money): StripeChargeLineItemType = {
          val entity = table.where(_.codeType.name === seedEntity.codeType.name).headOption.getOrElse {
            table.insert(seedEntity)
          }

          StripeChargeLineItemType(entity, id, amount)
        }
      }
    }

    //
    // SavesAsLineItemTypeEntity members
    //
    override protected def modelWithNewEntity(charge: StripeChargeLineItemType, entity: LineItemTypeEntity) = {
      charge.copy(_entity=entity)
    }
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

case class StripeChargeLineItemType(
  _entity: LineItemTypeEntity,
  stripeId: String,
  chargeAmount: Money
) extends LineItemType[StripeCharge]
  with LineItemTypeEntityLenses[StripeChargeLineItemType]
  with LineItemTypeEntityGetters[StripeChargeLineItemType]
{
  def lineItems(resolvedItems: IndexedSeq[LineItem[_]], pendingResolution: IndexedSeq[LineItemType[_]]) = {
    // TODO(SER-499): resolve this thang
    None
  }

  //
  // LineItemTypeEntityLenses members
  //
  override protected lazy val entityLens = StripeChargeLineItemType.entityLens
}

object StripeChargeLineItemType {
  val entityLens = Lens[StripeChargeLineItemType, LineItemTypeEntity] (
    get =  charge => charge._entity,
    set = (charge, entity) => charge.copy(entity)
  )

  def apply(id: String, chargeAmount: Money): StripeChargeLineItemType = {
    StripeChargeLineItemType(defaultEntity, id, chargeAmount)
  }

  lazy val defaultEntity = LineItemTypeEntity(_desc = "Stripe Charge")
    .withNature(LineItemNature.Payment)
    .withCodeType(CodeType.StripeChargeLineItemType)
}

////////////////////////////////////////////////////////////////////////////////////////////////////

case class StripeChargeLineItem(
  _entity: LineItemEntity,
  itemType: StripeChargeLineItemType,
  maybeDescription: Option[String] = None,
  subItems: IndexedSeq[LineItem[_]] = IndexedSeq()

) extends LineItem[StripeCharge] {

  // TODO(SER-499): check back if it would make sense to move implementation up into [[LineItem]]
  override def description = maybeDescription.getOrElse(itemType.description)
  override def amount = itemType.chargeAmount

  override def transact: StripeCharge = {
    // TODO(SER-499): use actual StripePayment to charge
    null  //:(
  }
}
