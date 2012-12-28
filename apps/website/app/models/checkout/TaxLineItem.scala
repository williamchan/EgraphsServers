package models.checkout

import org.joda.money.Money
import models.enums.{CodeType, LineItemNature}
import scalaz.Lens
import com.google.inject.Inject
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import services.AppConfig

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
  override def subItems: Seq[LineItem[_]] = Nil

  override def toJson: String = {
    // TODO(SER-499): Use Json type, maybe even Option
    ""
  }





  override def transact(newCheckoutId: Long): TaxLineItem = {
    if (id <= 0) {

      /**
       * TODO(SER-499): remove type saving if not keeping item type for each tax item; is there
       * reason to persist the tax type when it doesn't relate a tax table to the line items table?
       */
      this.withItemType( itemType.insert() )
        .withCheckoutId( newCheckoutId )
        .insert()

    } else {
      this
    }
  }

  // TODO(SER-499): this is repeated a lot, try to refactor into trait
  def withItemType(newType: TaxLineItemType) = {
    this.withItemTypeId(newType.id).copy(_typeEntity = newType._entity)
  }


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