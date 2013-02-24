package models.checkout

import org.joda.money.{Money, CurrencyUnit}
import models.{PrintOrderStore, PrintOrder, Order, OrderStore}
import services.db.{Schema, CanInsertAndUpdateEntityThroughTransientServices, InsertsAndUpdatesAsEntity, HasTransientServices}
import services.AppConfig
import com.google.inject.Inject
import scalaz.Lens
import models.enums.PaymentStatus
import play.api.libs.json.{JsValue, Json}
import services.blobs.AccessPolicy


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
    val imageUrl = product.celebrity.profilePhoto.getSaved(AccessPolicy.Public).url
    val thisJson = jsonify(product.name, product.description, Some(id), Some(imageUrl))
    val printOrderJson = printOrderItem map (_.toJsonAsSubItem)
    Json.toJson(Seq(thisJson) ++ printOrderJson.toSeq)
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
  protected def printOrderItem = _printOrderItem orElse { services.printOrderItemServices.findByOrderId(domainObject.id) }

  private def orderFromType = _type map { _.order}

  private def orderFromDb = services.orderStore.findByLineItemId(id).headOption

  private def restoreItemType = services.orderTypeServices.findEntityById(itemTypeId).headOption map { entity =>
    EgraphOrderLineItemType.restore(entity, domainObject)
  }

  private def clearGivenItemType = this.copy(_type = None)

  /** saves the order and optionally the print order if chosen */
  private def withSavedOrder(checkout: Checkout): EgraphOrderLineItem = {
    def savedPrintOrder(forOrder: Order) = {
      val printItem: Option[PrintOrderLineItem] = PrintOrderLineItemType(forOrder).lineItems().toSeq.flatten.headOption
      printItem map { item => item.transactAsSubItem(checkout) }
    }

    val buyerId = checkout.buyerCustomer.id
    val recipientId = checkout.recipientCustomer map (_.id) getOrElse buyerId

    val savedOrder = domainObject.copy(
      lineItemId = Some(id),
      buyerId = buyerId,
      recipientId = recipientId,
      amountPaidInCurrency = this.amount.getAmount
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
  def create(itemType: EgraphOrderLineItemType, amount: Money, printItem: Option[PrintOrderLineItem] = None) = {
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new EgraphOrderLineItem( entity, Some(itemType), printItem )
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = new EgraphOrderLineItem(entity)
}