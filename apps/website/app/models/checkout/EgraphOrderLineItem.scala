package models.checkout

import org.joda.money.{Money, CurrencyUnit}
import models.{PrintOrder, Order, OrderStore}
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

  override def domainObject: Order = (orderFromType orElse orderFromDb) getOrElse (throw new IllegalArgumentException("Order required."))

  override def itemType: EgraphOrderLineItemType = (_type orElse restoreItemType) getOrElse (throw new IllegalArgumentException("EgraphOrderLineItemType required."))

  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() } else {
      val savedItem = this.withCheckoutId(checkout.id).insert()
      savedItem.withSavedOrder(checkout)
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

  private def withSavedOrder(checkout: Checkout) = {
    val buyerId = checkout.buyerCustomer.id
    val recipientId = checkout.recipientCustomer map (_.id) getOrElse buyerId

    val savedOrder = domainObject.copy(
      lineItemId = Some(id),
      buyerId = buyerId,
      recipientId = recipientId
    ).withPaymentStatus(PaymentStatus.Charged).save()

    if (itemType.framedPrint) {
      val printItem = PrintOrderLineItemType(savedOrder).lineItems().flatten.headOption
      printItem map { item => item.transactAsSubItem(checkout) }
    }

    this.clearGivenItemType // clear because its Order is no longer current
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