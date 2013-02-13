package models.checkout

import models.enums.{CheckoutCodeType, LineItemNature}
import models.{ProductStore, OrderStore, Order}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject
import services.db.{HasTransientServices, InsertsAndUpdates, Schema}
import services.AppConfig


case class EgraphOrderLineItemTypeServices @Inject() (
  schema: Schema,
  orderStore: OrderStore,
  productStore: ProductStore
) extends InsertsAndUpdates[LineItemTypeEntity] {
  import org.squeryl.PrimitiveTypeMode._
  override protected def table = schema.lineItemTypes

  def findOrCreateEntityByProductId(id: Long) = {
    import org.squeryl.PrimitiveTypeMode._
    for (product <- productStore.findById(id).headOption) yield {
      lazy val existingEntity = product.lineItemTypeId flatMap { typeId =>
        table.lookup(typeId).headOption
      }
      lazy val createEntity = insert(baseEntityByProductId(product.id))
      existingEntity getOrElse createEntity
    }
  }


  private def baseEntityByProductId(id: Long) = LineItemTypeEntity(
    "Egraph Order for product " + id,
    EgraphOrderLineItemType.nature,
    EgraphOrderLineItemType.codeType
  )


  def findEntityById(id: Long) = table.lookup(id)
}





// TODO(CE-13): implement actual EgraphOrderLineItemType
case class EgraphOrderLineItemType(
  productId: Long = 0L,
  recipientName: String = "",
  isGift: Boolean = false,
  desiredText: Option[String] = None,
  messageToCeleb: Option[String] = None,
  framedPrint: Boolean = false,
  @transient _services: EgraphOrderLineItemTypeServices = AppConfig.instance[EgraphOrderLineItemTypeServices]
)
	extends LineItemType[Order]
  with LineItemTypeEntityGetters[EgraphOrderLineItemType]
  with HasTransientServices[EgraphOrderLineItemTypeServices]
{

  import models.checkout.checkout.Conversions._
  import models.enums.{LineItemNature, CheckoutCodeType}

  override lazy val _entity = services.findOrCreateEntityByProductId(productId).get

  override def id = _entity.id

  override def toJson = ""

  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = Some(
    Seq( EgraphOrderLineItem(this, price.get) )
  )

  // TODO(CE-13): look into inventory batch use
  lazy val product = services.productStore.findById(productId).get
  lazy val order = Order(
    productId = productId,
    recipientName = recipientName,
    messageToCelebrity = messageToCeleb,
    requestedMessage = desiredText,
    inventoryBatchId = product.availableInventoryBatches.head.id
  )


  def price = services.productStore.findById(productId) map (_.price)
}




object EgraphOrderLineItemType {
  def codeType = CheckoutCodeType.EgraphOrder
  def nature = LineItemNature.Product

  def restore(entity: LineItemTypeEntity, order: Order, withPrint: Boolean = false) = {
    new EgraphOrderLineItemType(
      productId = order.productId,
      recipientName = order.recipientName,
      isGift = order.recipientId == order.buyerId,
      desiredText = order.requestedMessage,
      messageToCeleb = order.messageToCelebrity,
      framedPrint = withPrint
    )
  }
}
