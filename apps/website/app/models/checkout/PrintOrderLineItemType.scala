package models.checkout

import checkout.Conversions._
import com.google.inject.Inject
import models.enums.{LineItemNature, CheckoutCodeType}
import models.{Order, PrintOrder}
import services.db.{InsertsAndUpdates, HasTransientServices, Schema}
import services.AppConfig


case class PrintOrderLineItemTypeServices @Inject() (schema: Schema) extends InsertsAndUpdates[LineItemTypeEntity] {

  override protected def table = schema.lineItemTypes

  def findOrCreateEntity() = existingEntity getOrElse insert(baseEntity)

  private def existingEntity = {
    import org.squeryl.PrimitiveTypeMode._
    table.where(_._codeType === PrintOrderLineItemType.codeType.name).headOption
  }

  private def baseEntity = LineItemTypeEntity(
    "Order of Framed Print",
    PrintOrderLineItemType.nature,
    PrintOrderLineItemType.codeType
  )
}




/**
 * Singleton LineItemType for PrintOrders.
 *
 * Will be singleton because a PrintOrder naturally has a 1-to-1 mapping to LineItems,
 * so having multiple LineItemTypes would be redundant. (Compare to coupons which have
 * a one-to-many mapping onto LineItems since they can potentially be used multiple times,
 * so they map onto a set of LineItems via some LineItemType.)
 *
 * TODO(CE-13): reconsider if there is value to having a separate LineItemType for each product's
 * print order; definitely leave room to add different types of physical products (ex: only require
 * renaming to FramedPrintOrder or something).
 */
case class PrintOrderLineItemType(
  forOrder: Order = Order(), // could be just order
  @transient _services: PrintOrderLineItemTypeServices = AppConfig.instance[PrintOrderLineItemTypeServices]
)
  extends LineItemType[PrintOrder]
  with LineItemTypeEntityGetters[PrintOrderLineItemType]
  with HasTransientServices[PrintOrderLineItemTypeServices]
{

  override lazy val _entity = services.findOrCreateEntity()

  override def id = _entity.id

  override def lineItems(resolved: LineItems = Nil, unresolved: LineItemTypes = Nil): Option[Seq[PrintOrderLineItem]] = Some {
    Seq( PrintOrderLineItem(this, printOrder) )
  }

  override def toJson = ""

  def printOrder = PrintOrder(orderId = forOrder.id)
}




object PrintOrderLineItemType {
  def codeType = CheckoutCodeType.PrintOrder
  def nature = LineItemNature.Product
}
