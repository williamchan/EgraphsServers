package models.checkout

import models.enums.{CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.Lens
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import services.payment.StripeCharge
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import com.google.inject.Inject


case class StripeChargeLineItemType(
  _entity: LineItemTypeEntity,
  stripeToken: String,
  services: StripeChargeLineItemTypeServices = AppConfig.instance[StripeChargeLineItemTypeServices]
) extends LineItemType[StripeCharge] with HasLineItemTypeEntity
  with LineItemTypeEntityGettersAndSetters[StripeChargeLineItemType]
  with CanInsertAndUpdateAsThroughServices[StripeChargeLineItemType, LineItemTypeEntity]
{
  override def toJson: String = {
    // TODO(SER-499): more json
    ""
  }

  def lineItems(resolvedItems: Seq[LineItem[_]], pendingResolution: Seq[LineItemType[_]]) = {
    resolvedItems.find( item => item.itemType.codeType == CodeType.Total) match {
      case Some(total: TotalLineItem) => Seq( StripeChargeLineItem(this, total.amount.negated) )
      case _ => Nil
    }
  }

  override protected lazy val entityLens = Lens[StripeChargeLineItemType, LineItemTypeEntity] (
    get =  charge => charge._entity,
    set = (charge, entity) => charge.copy(entity)
  )
}

object StripeChargeLineItemType {
  val nature = LineItemNature.Charge
  val codeType = CodeType.StripeCharge

  def apply(stripeToken: String): StripeChargeLineItemType = {
    StripeChargeLineItemType(
      new LineItemTypeEntity("Stripe Charge", nature, codeType),
      stripeToken
    )
  }
}





case class StripeChargeLineItemTypeServices @Inject() (
  schema: Schema
) extends SavesAsLineItemTypeEntity[StripeChargeLineItemType] {

  // TODO(SER-499): determine what additional services are needed

  override protected def modelWithNewEntity(charge: StripeChargeLineItemType, entity: LineItemTypeEntity) = {
    charge.copy(_entity = entity)
  }
}