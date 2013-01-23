package models.checkout

import checkout.Conversions._
import com.google.inject.Inject
import org.joda.money.Money
import scalaz.Lens
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import services.AppConfig


// TODO: see TaxLineItemType


case class TaxLineItem private (
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  services: TaxLineItemServices = AppConfig.instance[TaxLineItemServices]
) extends LineItem[Money] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[TaxLineItem]
  with CanInsertAndUpdateAsThroughServices[TaxLineItem, LineItemEntity]
{
  require(amount.isPositiveOrZero)

  override def itemType: TaxLineItemType = TaxLineItemType(_typeEntity, _entity)
  override def domainObject: Money = amount
  override def subItems: LineItems = Nil

  override def toJson: String = {
    // TODO(SER-499): Use Json type, maybe even Option
    ""
  }





  override def transact(checkout: Checkout): TaxLineItem = {
    if (id <= 0) {
      this.withItemType(itemType.insert())
        .withCheckoutId(checkout.id)
        .insert()

    } else {
      this
    }
  }


  def withItemType(newType: TaxLineItemType) = {
    this.withItemTypeId(newType.id).copy(_typeEntity = newType._entity)
  }


  //
  // EntityLens members
  //
  override protected lazy val entityLens = Lens[TaxLineItem, LineItemEntity] (
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )
}




object TaxLineItem {
  def apply(itemType: TaxLineItemType, amount: Money) = {
    new TaxLineItem(
      LineItemEntity(_amountInCurrency = amount.getAmount),
      itemType._entity
    )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    new TaxLineItem(entity, typeEntity)
  }
}







case class TaxLineItemServices @Inject() (
  schema: Schema
) extends SavesAsLineItemEntity[TaxLineItem] {
  override protected def modelWithNewEntity(tax: TaxLineItem, entity: LineItemEntity) = {
    tax.copy(_entity=entity)
  }
}