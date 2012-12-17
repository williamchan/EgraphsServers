package models.checkout

import org.joda.money.Money
import models.enums.{CodeType, LineItemNature}
import scalaz.Lens

case class TaxLineItem private (
  _entity: LineItemEntity,
  itemType: TaxLineItemType
) extends LineItem[Money] with HasLineItemEntity
  with LineItemEntityLenses[TaxLineItem]
  with LineItemEntityGetters[TaxLineItem]
  with LineItemEntitySetters[TaxLineItem]
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
    require(checkoutId > 0, "Cannot transact without setting checkoutId.")

    if (id == checkout.UnsavedEntity) {
      import TaxLineItemTypeServices.Conversions._
      import TaxLineItemServices.Conversions._

      /**
       * TODO(SER-499): remove type saving if not keeping item type for each tax item; is there
       * reason to persist the tax type when it doesn't relate a tax table to the line items table?
       */
      withItemType(itemType.create()).create()
    } else {
      this
    }
  }

  // TODO(SER-499): this is repeated a lot, try to refactor into trait
  def withItemType(newType: TaxLineItemType) = {
    this.copy(itemType = newType).withItemTypeId(newType.id)
  }


  override def _domainEntityId: Long = checkout.UnusedDomainEntity // NOTE(SER-499): unused


  override protected lazy val entityLens = Lens[TaxLineItem, LineItemEntity] (
    get = tax => tax._entity,
    set = (tax, entity) => tax.copy(entity)
  )
}

object TaxLineItem {
  def apply(itemType: TaxLineItemType, amount: Money) = {
    new TaxLineItem(
      new LineItemEntity(amount.getAmount),
      itemType
    )
  }


}

