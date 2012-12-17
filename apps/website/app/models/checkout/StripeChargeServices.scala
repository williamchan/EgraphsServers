package models.checkout

import services.db.Schema
import org.joda.money.Money


object StripeChargeLineItemServices
  extends LineItemComponent.SavesAsLineItemEntity[StripeChargeLineItem]
{
  object Conversions extends LineItemSavingConversions

  override protected def modelWithNewEntity(charge: StripeChargeLineItem, entity: LineItemEntity) = {
    charge.copy(_entity = entity)
  }
}


object StripeChargeLineItemTypeServices
  extends LineItemComponent.SavesAsLineItemTypeEntity[StripeChargeLineItemType]
{
  object Conversions extends LineItemTypeSavingConversions

  //
  // SavesAsLineItemTypeEntity members
  //
  override protected def modelWithNewEntity(charge: StripeChargeLineItemType, entity: LineItemTypeEntity) = {
    charge.copy(_entity = entity)
  }
}





