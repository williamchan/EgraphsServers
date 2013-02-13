package models.checkout

import models.{OrderStore, PrintOrderStore, PrintOrder}
import com.google.inject.Inject
import services.db.Schema
import services.AppConfig
import scalaz.Lens


//
// Model
//
case class PrintOrderLineItem(
  _entity: LineItemEntity = LineItemEntity(),
  _type: Option[PrintOrderLineItemType] = None,
  _printOrder: Option[PrintOrder] = None,
  @transient _services: PrintOrderLineItemServices = AppConfig.instance[PrintOrderLineItemServices]
)
  extends SubLineItem[PrintOrder]
  with HasLineItemEntity[PrintOrderLineItem]
  with LineItemEntityGettersAndSetters[PrintOrderLineItem]
  with SavesAsLineItemEntityThroughServices[PrintOrderLineItem, PrintOrderLineItemServices]
{

  //
  // LineItem members
  //
  override def toJson = ""
  override def itemType = (_type orElse typeFromOrder) get
  override def domainObject = (_printOrder orElse printOrderFromDb) getOrElse {
    println("whaaat?")
    _printOrder.get
  }

  //
  // SubLineItem members
  //
  override def transactAsSubItem(checkout: Checkout) = {
    require(domainObject.orderId > 0, "Cannot transact PrintOrder for nonexistant Order")
    if (id > 0) { this.update() }
    else {
      val savedItem = this.withCheckoutId(checkout.id).insert()
      savedItem.withSavedPrintOrder(checkout)
    }
  }


  //
  // EntityLenses members
  //
  override def entityLens = Lens[PrintOrderLineItem, LineItemEntity](get = _._entity, set = _ copy _)


  //
  // Helpers
  //
  /** get `PrintOrder` by LineItemId */
  private def printOrderFromDb = services.printOrderStore.findByLineItemId(id).headOption

  /** get `Order` of the `PrintOrder` and create the itemType from that */
  private def typeFromOrder = services.orderStore.findById(domainObject.orderId).headOption map { order =>
    PrintOrderLineItemType(order)
  }

  /** save `PrintOrder` and copy into _printOrder */
  private def withSavedPrintOrder(checkout: Checkout) = {
    // TODO(CE-13): add name to shipping address
    val savedPrint = domainObject.copy(lineItemId = Some(id))
      .withShippingAddress(checkout.shippingAddress.get)
      .save()
    this.copy(_printOrder = Some(savedPrint))
  }
}


//
// Companion
//
object PrintOrderLineItem {
  def apply(itemType: PrintOrderLineItemType, printOrder: PrintOrder) = {
    import services.Finance.TypeConversions._
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(PrintOrder.pricePerPrint.toMoney())
    new PrintOrderLineItem(entity, Some(itemType), Some(printOrder))
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity): PrintOrderLineItem = {
    new PrintOrderLineItem(entity)
  }
}


//
// Services
//
case class PrintOrderLineItemServices @Inject() (
  schema: Schema,
  printOrderStore: PrintOrderStore,
  orderStore: OrderStore
) extends SavesAsLineItemEntity[PrintOrderLineItem] {

}