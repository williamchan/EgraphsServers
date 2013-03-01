package models.checkout

import models.checkout.Conversions._
import models.enums.{CheckoutCodeType, LineItemNature}
import models._
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
  addressForPrint: Option[String] = None,
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
    val item = EgraphOrderLineItem.create(this, price)
    val print = item.printOrderItem
    Seq(item) ++ print.toSeq
  }

  //
  // Helpers
  //
  lazy val order = Order(
    productId = productId,
    recipientName = recipientName,
    messageToCelebrity = messageToCeleb,
    requestedMessage = desiredText,
    inventoryBatchId = inventoryBatch.id // TODO(CE-13): move inventory batch check to EgraphOrderLineItem#transact
  )

  lazy val product = services.productStore.findById(productId) getOrElse {
    throw new IllegalArgumentException("Valid product id required.")
  }

  def price = product.price

  def inventoryBatch = product.availableInventoryBatches find (_.hasInventory) getOrElse {
    // TODO(CE-13): remove this from here, move to transact as above
    throw new IllegalArgumentException("Product is out of inventory.")
  }

  def withoutPrint = copy(framedPrint = false, addressForPrint = None)
  def withPrint = this.copy(framedPrint = true)
  def withShippingAddress(shippingAddress: String) = this.copy(addressForPrint = Some(shippingAddress))
}


//
// Companion
//
object EgraphOrderLineItemType {
  def codeType = CheckoutCodeType.EgraphOrder
  def nature = LineItemNature.Product

  def restore(entity: LineItemTypeEntity, order: Order): EgraphOrderLineItemType = {
    val isGift = order.recipientId != order.buyerId
    val desiredText = order.requestedMessage
    new EgraphOrderLineItemType(order.productId, order.recipientName, isGift, desiredText, order.messageToCelebrity)
  }
}
