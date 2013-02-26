package models.checkout

import models.{Order, OrderStore, PrintOrderStore, PrintOrder}
import com.google.inject.Inject
import services.db.Schema
import services.AppConfig
import scalaz.Lens
import exception.{DomainObjectNotFoundException, ItemTypeNotFoundException, MissingRequiredAddressException}
import play.api.libs.json.Json
import models.enums.CheckoutCodeType


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
  override def itemType = (_type orElse typeFromOrder) getOrElse (throw new ItemTypeNotFoundException("PrintOrderLineItemType"))
  override def domainObject = (_printOrder orElse printOrderFromDb) getOrElse (throw new DomainObjectNotFoundException("PrintOrder"))


  //
  // SubLineItem members
  //
  override def transactAsSubItem(checkout: Checkout): PrintOrderLineItem = {
    require(domainObject.orderId > 0, "Cannot transact PrintOrder for nonexistant Order")
    if (id > 0) { this.update() }
    else {
      val savedItem = this.withCheckoutId(checkout.id).insert()
      savedItem.withSavedPrintOrder(checkout)
    }
  }

  override def toJsonAsSubItem = jsonify(
    name = "High quality print",
    description = "Framed print of digital egraph",
    id = Some(id),
    imageUrl = Some("/assets/images/framed-print.png")
  )


  //
  // EntityLenses member
  //
  override def entityLens = Lens[PrintOrderLineItem, LineItemEntity](get = _._entity, set = _ copy _)


  //
  // Helpers
  //
  /** get `PrintOrder` by LineItemId */
  private def printOrderFromDb = services.printOrderStore.findByLineItemId(id).headOption

  /** itemType must be provided or requires an order, so this method recreates it from the order of the printOrder */
  private def typeFromOrder = services.orderStore.findById(domainObject.orderId).headOption map { order =>
    PrintOrderLineItemType(order)
  }

  /** saves print order */
  private def withSavedPrintOrder(checkout: Checkout) = {
    val address = (givenShippingAddress orElse checkout.shippingAddress) getOrElse {
      throw new MissingRequiredAddressException("PrintOrderLineItemType")
    }

    val savedPrint = domainObject.copy(
      shippingAddress = address,
      lineItemId = Some(id),
      amountPaidInCurrency = this.amount.getAmount
    ).save()

    this.copy(_printOrder = Some(savedPrint))
  }

  private def givenShippingAddress = optionIf(!domainObject.shippingAddress.isEmpty) { domainObject.shippingAddress }

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
  orderStore: OrderStore,
  itemStore: LineItemStore
) extends SavesAsLineItemEntity[PrintOrderLineItem] {

  def findByOrderId(orderId: Long) = {
    for (
      order <- orderStore.findById(orderId);
      itemId <- order.lineItemId;
      item <- itemStore.findByIdWithCodeType(itemId, CheckoutCodeType.PrintOrder)
    ) yield item
  }
}