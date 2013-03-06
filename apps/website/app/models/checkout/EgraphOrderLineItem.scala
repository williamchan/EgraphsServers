package models.checkout

import org.joda.money.Money
import models.{Order, OrderStore}
import services.db.Schema
import services.AppConfig
import com.google.inject.Inject
import models.enums.PaymentStatus
import play.api.libs.json.Json
import services.blobs.AccessPolicy
import models.ImageAsset.Jpeg


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
    val imageUrl = product.photo.resizedWidth(60).withImageType(Jpeg).getSaved(AccessPolicy.Public).url
    val name = s"Online Egraph from ${product.celebrity.publicName}"
    val description = s"For ${domainObject.recipientName}. Photo: ${product.name}."
    val thisJson = jsonify(name, description, Some(id), Some(imageUrl))
    val printOrderJson = printOrderItem map (_.toJsonAsSubItem)
    Json.toJson(Seq(thisJson) ++ printOrderJson.toSeq)
  }

  override lazy val domainObject: Order = (orderFromDb orElse orderFromType) getOrElse {
    throw new IllegalArgumentException("Order required.")
  }

  override lazy val itemType: EgraphOrderLineItemType = (_type orElse restoreItemType) getOrElse {
    throw new IllegalArgumentException("EgraphOrderLineItemType required.")
  }

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
  protected[checkout] lazy val printOrderItem = optionIf (itemType.framedPrint) {
    PrintOrderLineItemType(domainObject).lineItemsAsSubType.headOption
  }.flatten


  private def orderFromType = _type map { _.order}

  private def orderFromDb = services.orderStore.findByLineItemId(id).headOption

  private def restoreItemType = services.orderTypeServices.findEntityById(itemTypeId).headOption map { entity =>
    EgraphOrderLineItemType.restore(entity, domainObject)
  }

  /** saves the order and optionally the print order if chosen */
  private def withSavedOrder(checkout: Checkout): EgraphOrderLineItem = {
    val buyer = checkout.buyerCustomer
    val recipient = checkout.recipientCustomer getOrElse buyer

    // buyer "buys" order
    val savedOrder = buyer.buy(
      product = itemType.product,
      recipient = recipient,
      recipientName = itemType.recipientName,
      messageToCelebrity = itemType.messageToCeleb,
      requestedMessage = itemType.desiredText
    ) match {
      case Left(error) => throw error                         // hacky, TODO: refactor transact to use Either
      case Right(order) => order.copy(
        lineItemId = Some(id)
      ).withPaymentStatus(PaymentStatus.Charged).save()
    }

    // save print if necessary
    if (itemType.framedPrint) {
      PrintOrderLineItemType(savedOrder).lineItemsAsSubType.head
        .transactAsSubItem(checkout)
    }

    this.clearGivenItemType  // clear _type because orderFromType is no longer current
  }

  private def clearGivenItemType = this.copy(_type = None)
}


//
// Companion
//
object EgraphOrderLineItem {
  def create(itemType: EgraphOrderLineItemType, amount: Money) = {
    val entity = LineItemEntity(_itemTypeId = itemType.id).withAmount(amount)
    new EgraphOrderLineItem( entity, Some(itemType))
  }

  def apply(entity: LineItemEntity, typeEntity: LineItemTypeEntity) = new EgraphOrderLineItem(entity)
}