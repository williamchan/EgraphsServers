package models.checkout

import models.enums.{CodeType, LineItemNature}
import org.joda.money.Money
import scalaz.Lens
import services.db.Schema
import services.payment.StripeCharge
import org.squeryl.PrimitiveTypeMode._


case class StripeChargeLineItemType(
  _entity: LineItemTypeEntity,
  stripeToken: String
) extends LineItemType[StripeCharge] with HasLineItemTypeEntity
  with LineItemTypeEntityLenses[StripeChargeLineItemType]
  with LineItemTypeEntityGetters[StripeChargeLineItemType]
  with LineItemTypeEntitySetters[StripeChargeLineItemType]
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

