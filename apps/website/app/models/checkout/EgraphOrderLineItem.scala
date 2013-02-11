package models.checkout

import org.joda.money.{Money, CurrencyUnit}
import models.{Order, OrderStore}
import services.db.{Schema, CanInsertAndUpdateAsThroughTransientServices, InsertsAndUpdatesAsEntity, HasTransientServices}
import services.AppConfig
import com.google.inject.Inject
import scalaz.Lens


case class EgraphOrderLineItemServices @Inject() (
  schema: Schema,
  orderStore: OrderStore
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
  override def domainObject =  (orderFromType orElse orderFromDb) get
  override def itemType: EgraphOrderLineItemType = _type.get
  override def transact(checkout: Checkout) = this.withCheckoutId(checkout.id).insert()
  override def toJson = ""


  //
  // Helpers
  //
  private def orderFromType = _type map { _.order}

  private def orderFromDb = services.orderStore.findByLineItemId(id).headOption

  override protected lazy val entityLens = EntityLens(
    get = order => order._entity,
    set = (order, entity) => order.copy(entity)
  )
}


object EgraphOrderLineItem {
  def apply(itemType: EgraphOrderLineItemType, amount: Money) = {
    val entity = LineItemEntity(0, 0, itemType.id).withAmount(amount)
    new EgraphOrderLineItem( entity, Some(itemType) )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = new EgraphOrderLineItem(entity)
}