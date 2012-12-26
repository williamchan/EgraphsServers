package models.checkout

import org.joda.money.Money
import models.enums.{CodeType, LineItemNature}
import scalaz.Lens
import com.google.inject.Inject
import services.db.{CanInsertAndUpdateAsThroughServices, Schema}
import services.AppConfig

case class TaxLineItem private (
  _entity: LineItemEntity,
  itemType: TaxLineItemType,
  services: TaxLineItemServices = AppConfig.instance[TaxLineItemServices]
) extends LineItem[Money] with HasLineItemEntity
  with LineItemEntityGettersAndSetters[TaxLineItem]
  with CanInsertAndUpdateAsThroughServices[TaxLineItem, LineItemEntity]
{
  require(amount.isPositiveOrZero)

  override def domainObject: Money = amount


  override def toJson: String = {
    // TODO(SER-499): Use Json type, maybe even Option
    ""
  }


  // NOTE(SER-499): will probably want to use subItems for non-trivial tax scenarios
  override def subItems: Seq[LineItem[_]] = Nil


  override def transact: LineItem[Money] = {
    if (id <= 0) {
      require(checkoutId > 0, "Cannot transact without setting checkoutId.")

      /**
       * TODO(SER-499): remove type saving if not keeping item type for each tax item; is there
       * reason to persist the tax type when it doesn't relate a tax table to the line items table?
       */
      withItemType(itemType.insert()).insert()
    } else {
      this
    }
  }

  // TODO(SER-499): this is repeated a lot, try to refactor into trait
  def withItemType(newType: TaxLineItemType) = {
    this.copy(itemType = newType).withItemTypeId(newType.id)
  }


  override protected lazy val entityLens = Lens[TaxLineItem, LineItemEntity] (
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )
}

object TaxLineItem {
  def apply(itemType: TaxLineItemType, amount: Money, maybeZip: Option[String]) = {
    new TaxLineItem(
      LineItemEntity(amount, maybeZip.getOrElse(TaxLineItemType.noZipcode)),
      itemType
    )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = {
    val itemType = TaxLineItemType(BigDecimal(0.0), None)
//      services.lineItemTypeStore.entitiesToType[TaxLineItemType](typeEntity,entity).getOrElse(
//        throw new IllegalArgumentException("Could not create a TaxLineItemType from the given entity.")
//      )

    new TaxLineItem(entity, itemType)
  }
}







case class TaxLineItemServices @Inject() (
  schema: Schema,
  lineItemTypeStore: LineItemTypeStore
) extends SavesAsLineItemEntity[TaxLineItem] {
  override protected def modelWithNewEntity(tax: TaxLineItem, entity: LineItemEntity) = {
    tax.copy(_entity=entity)
  }
}