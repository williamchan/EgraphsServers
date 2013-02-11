package models.checkout

import models.PrintOrder
import com.google.inject.Inject
import services.db.Schema
import services.AppConfig
import scalaz.Lens

case class PrintOrderLineItem(
  _entity: LineItemEntity = LineItemEntity(),
  _type: Option[PrintOrderLineItemType] = None,
  _printOrder: Option[PrintOrder] = None,
  @transient _services: PrintOrderLineItemServices = AppConfig.instance[PrintOrderLineItemServices]
)
  extends LineItem[PrintOrder]
  with HasLineItemEntity[PrintOrderLineItem]
  with LineItemEntityGettersAndSetters[PrintOrderLineItem]
  with SavesAsLineItemEntityThroughServices[PrintOrderLineItem, PrintOrderLineItemServices]
{

  override def toJson = ""
  override def transact(checkout: Checkout) = this
  override def itemType = _type.get
  override def domainObject = _printOrder.get

  override def entityLens = Lens[PrintOrderLineItem, LineItemEntity](get = _._entity, set = _ copy _)

}

object PrintOrderLineItem {
  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity): PrintOrderLineItem = {
    new PrintOrderLineItem()
  }
}

case class PrintOrderLineItemServices @Inject() (
  schema: Schema
) extends SavesAsLineItemEntity[PrintOrderLineItem] {

}