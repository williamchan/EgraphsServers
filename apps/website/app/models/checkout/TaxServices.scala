package models.checkout

object TaxLineItemServices extends LineItemComponent.SavesAsLineItemEntity[TaxLineItem] {
  object Conversions extends LineItemSavingConversions

  def modelWithNewEntity(tax: TaxLineItem, entity: LineItemEntity) = {
    tax.copy(_entity = entity)
  }
}

object TaxLineItemTypeServices extends LineItemComponent.SavesAsLineItemTypeEntity[TaxLineItemType] {
  object Conversions extends LineItemTypeSavingConversions

  def modelWithNewEntity(tax: TaxLineItemType, entity: LineItemTypeEntity) = {
    tax.copy(_entity = entity)
  }
}