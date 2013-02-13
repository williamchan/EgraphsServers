package models.checkout

import checkout.Conversions._
import com.google.inject.Inject
import org.joda.money.Money
import scalaz.Lens
import services.db.{CanInsertAndUpdateEntityThroughServices, Schema}
import services.AppConfig


// TODO(SER-499): this stuff is functional, but has no tests. Will finish when done with Checkout Explorations, or possibly during the later stages of it.


//
// Model
//
case class TaxLineItem private (
  _entity: LineItemEntity,
  _typeEntity: LineItemTypeEntity,
  @transient _services: TaxLineItemServices = AppConfig.instance[TaxLineItemServices]
)
	extends LineItem[Money]
	with HasLineItemEntity[TaxLineItem]
  with LineItemEntityGettersAndSetters[TaxLineItem]
  with SavesAsLineItemEntityThroughServices[TaxLineItem, TaxLineItemServices]
{
  require(amount.isPositiveOrZero)

  override def itemType: TaxLineItemType = TaxLineItemType(_typeEntity, _entity)
  override def domainObject: Money = amount


  override def toJson: String = ""


  override def transact(checkout: Checkout): TaxLineItem = {
    if (id > 0) { this }
    else {
      this.withItemType(itemType.insert())
        .withCheckoutId(checkout.id)
        .insert()
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



//
// Companion object
//
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






//
// Services
//
case class TaxLineItemServices @Inject() (
  schema: Schema
) extends SavesAsLineItemEntity[TaxLineItem]