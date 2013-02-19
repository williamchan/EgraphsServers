package models.checkout

import org.joda.money.{Money, CurrencyUnit}
import models.{PrintOrderStore, PrintOrder, Order, OrderStore}
import services.db.{Schema, CanInsertAndUpdateEntityThroughTransientServices, InsertsAndUpdatesAsEntity, HasTransientServices}
import services.AppConfig
import com.google.inject.Inject
import scalaz.Lens
import models.enums.PaymentStatus
import play.api.libs.json.{JsValue, Json}


//
// Services
//
case class EgraphOrderLineItemServices @Inject() (
  schema: Schema,
  orderStore: OrderStore,
  orderTypeServices: EgraphOrderLineItemTypeServices,
  printOrderItemServices: PrintOrderLineItemServices
) extends SavesAsLineItemEntity[EgraphOrderLineItem]


//
// Models
//
case class EgraphOrderLineItem(
  _entity: LineItemEntity = LineItemEntity(),
  _type: Option[EgraphOrderLineItemType] = None,
  _printOrderItem: Option[PrintOrderLineItem] = None,
  @transient _services: EgraphOrderLineItemServices = AppConfig.instance[EgraphOrderLineItemServices]
) extends LineItem[Order]
  with HasLineItemEntity[EgraphOrderLineItem]
  with SavesAsLineItemEntityThroughServices[EgraphOrderLineItem, EgraphOrderLineItemServices]
  with LineItemEntityGettersAndSetters[EgraphOrderLineItem]
{

  //
  // LineItem members
  //
  /** includes print order item's json if it exists */
  override def toJson = {
    val product = domainObject.product
    val thisJson = jsonify(product.name, product.description, Some(id), Some(product.defaultIcon.url))
    val printOrderJson = printOrderItem map (_.toJson)
    Json.toJson(printOrderJson.toSeq ++ Seq(thisJson))
  }

  override def domainObject: Order = (orderFromType orElse orderFromDb) getOrElse (throw new IllegalArgumentException("Order required."))

  override def itemType: EgraphOrderLineItemType = (_type orElse restoreItemType) getOrElse (throw new IllegalArgumentException("EgraphOrderLineItemType required."))

  override def transact(checkout: Checkout) = {
    if (id > 0) { this.update() } else {
      val savedItem = this.withCheckoutId(checkout.id).insert()
      savedItem.withSavedOrder(checkout)
    }
  }

  //
  // LineItemEntityLenses member
  //
  override protected lazy val entityLens = EntityLens(
    get = order => order._entity,
    set = (order, entity) => order.copy(entity)
  )

  //
  // Helpers
  //
  /** get actual print order */
  def printOrderItem = _printOrderItem orElse { services.printOrderItemServices.findByOrderId(domainObject.id) }

  private def orderFromType = _type map { _.order}

  private def orderFromDb = services.orderStore.findByLineItemId(id).headOption

  private def restoreItemType = services.orderTypeServices.findEntityById(itemTypeId).headOption map { entity =>
    EgraphOrderLineItemType.restore(entity, domainObject)
  }

  private def clearGivenItemType = this.copy(_type = None)

  /** saves the order and optionally the print order if chosen */
  private def withSavedOrder(checkout: Checkout): EgraphOrderLineItem = {
    def savedPrintOrder(forOrder: Order) = {
      val printItem = PrintOrderLineItemType(forOrder).lineItems().flatten.headOption
      printItem map { item => item.transactAsSubItem(checkout) }
    }

    val buyerId = checkout.buyerCustomer.id
    val recipientId = checkout.recipientCustomer map (_.id) getOrElse buyerId

    val savedOrder = domainObject.copy(
      lineItemId = Some(id),
      buyerId = buyerId,
      recipientId = recipientId
    ).withPaymentStatus(PaymentStatus.Charged).save()

    /**
     * Save print order if framedPrint flag is true; the PrintOrder line item that appears in the checkouts lineitems
     * is a dummy item for informative purposes as of CE-13.
     */
    val savedPrintOrderItem = if (itemType.framedPrint) savedPrintOrder(savedOrder) else None

    this.clearGivenItemType // clear because its Order is no longer current
      .copy(_printOrderItem = savedPrintOrderItem)
  }
}


//
// Companion
//
object EgraphOrderLineItem {
  def apply(itemType: EgraphOrderLineItemType, amount: Money) = {
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new EgraphOrderLineItem( entity, Some(itemType) )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = new EgraphOrderLineItem(entity)
}