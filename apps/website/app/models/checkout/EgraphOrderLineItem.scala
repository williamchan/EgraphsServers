package models.checkout

import org.joda.money.{Money, CurrencyUnit}
import models.{Order, OrderStore}
import services.db.{Schema, CanInsertAndUpdateEntityThroughTransientServices, InsertsAndUpdatesAsEntity, HasTransientServices}
import services.AppConfig
import com.google.inject.Inject
import scalaz.Lens
import models.enums.PaymentStatus


case class EgraphOrderLineItemServices @Inject() (
  schema: Schema,
  orderStore: OrderStore,
  orderTypeServices: EgraphOrderLineItemTypeServices
) extends SavesAsLineItemEntity[EgraphOrderLineItem] {

}


case class EgraphOrderLineItem(
  _entity: LineItemEntity = LineItemEntity(),
  _type: Option[EgraphOrderLineItemType] = None,
  @transient _services: EgraphOrderLineItemServices = AppConfig.instance[EgraphOrderLineItemServices]
)
	extends LineItem[Order]
	with HasLineItemEntity[EgraphOrderLineItem]
  with SavesAsLineItemEntityThroughServices[EgraphOrderLineItem, EgraphOrderLineItemServices]
  with LineItemEntityGettersAndSetters[EgraphOrderLineItem]
{

  //
  // LineItem members
  //
  override def toJson = ""

  override def domainObject: Order = (orderFromType orElse orderFromDb) get

  override def itemType: EgraphOrderLineItemType = (_type orElse restoreItemType) get

  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() } else {
      val savedItem = this.withCheckoutId(checkout.id).insert()
      savedItem.saveOrder(checkout)
      savedItem.clearGivenItemType  // clear because _type's order is no longer current
    }
  }


  //
  // Helpers
  //
  private def orderFromType = _type map { _.order}
  private def orderFromDb = services.orderStore.findByLineItemId(id).headOption
  private def restoreItemType = services.orderTypeServices.findEntityById(itemTypeId).headOption map { entity =>
    EgraphOrderLineItemType.restore(entity, domainObject)
  }

  private def clearGivenItemType = this.copy(_type = None)
  private def saveOrder(checkout: Checkout) = {
    val buyerId = checkout.buyerId
    val recipientId = checkout.recipient map (_.id) getOrElse buyerId
    domainObject.copy(
      lineItemId = Some(id),
      buyerId = buyerId,
      recipientId = recipientId
    ).withPaymentStatus(PaymentStatus.Charged).save()
  }

  override protected lazy val entityLens = EntityLens(
    get = order => order._entity,
    set = (order, entity) => order.copy(entity)
  )
}


object EgraphOrderLineItem {
  def apply(itemType: EgraphOrderLineItemType, amount: Money) = {
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new EgraphOrderLineItem( entity, Some(itemType) )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = new EgraphOrderLineItem(entity)
}