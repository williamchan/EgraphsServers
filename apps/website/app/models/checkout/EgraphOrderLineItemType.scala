package models.checkout

import checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}
import models.{ProductStore, OrderStore, Order, Product}
import org.joda.money.{CurrencyUnit, Money}
import com.google.inject.Inject
import services.db.{HasTransientServices, InsertsAndUpdates, Schema}
import services.AppConfig


//
// Services
//
case class EgraphOrderLineItemTypeServices @Inject() (
  schema: Schema,
  orderStore: OrderStore,
  productStore: ProductStore
) extends InsertsAndUpdates[LineItemTypeEntity] {
  import org.squeryl.PrimitiveTypeMode._
  override protected def table = schema.lineItemTypes

  def findOrCreateEntityForProduct(product: Product): LineItemTypeEntity = {
    def existingEntity = product.lineItemTypeId flatMap { typeId => table.lookup(typeId).headOption }
    def createEntity = {
      val entity = insert( baseEntityByProduct(product) )
      product.copy(lineItemTypeId = Some(entity.id)).save()
      entity
    }
    existingEntity getOrElse createEntity
  }

  private def baseEntityByProduct(product: Product) = LineItemTypeEntity(
    "Egraph Order for product " + product.id,
    EgraphOrderLineItemType.nature,
    EgraphOrderLineItemType.codeType
  )

  def findEntityById(id: Long) = table.lookup(id)
}


//
// Model
//
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
  //
  // HasLineItemTypeEntity member
  //
  override lazy val _entity = services.findOrCreateEntityForProduct(product)

  //
  // LineItemType members
  //
  override def id = _entity.id

  override def lineItems(resolvedItems: LineItems, pendingResolution: LineItemTypes) = Some {
    val print: LineItems = if (!framedPrint) Nil else PrintOrderLineItemType(order).lineItems().getOrElse(Nil)
    print ++ Seq( EgraphOrderLineItem(this, price) )
  }

  //
  // Helpers
  //
  lazy val order = Order(
    productId = productId,
    recipientName = recipientName,
    messageToCelebrity = messageToCeleb,
    requestedMessage = desiredText,

    // TODO(CE-13): this may be redundant, and may cause a problem if the product sells out between navigating to the product page and adding to the checkout -- should make sure to be checking product inventory at transaction
    inventoryBatchId = inventoryBatch.id
  )

  lazy val product = services.productStore.findById(productId) getOrElse {
    throw new IllegalArgumentException("Valid product id required.")
  }

  def price = product.price

  def inventoryBatch = product.availableInventoryBatches find (_.hasInventory) getOrElse {
    throw new IllegalArgumentException("Product is out of inventory.")
  }
}


//
// Companion
//
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
