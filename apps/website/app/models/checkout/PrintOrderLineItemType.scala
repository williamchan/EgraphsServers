package models.checkout

import checkout.Conversions._
import com.google.inject.Inject
import models.enums.{LineItemNature, CheckoutCodeType}
import models.PrintOrder
import services.db.{InsertsAndUpdates, HasTransientServices, Schema}
import scalaz.Lens
import services.AppConfig

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
  egraphOrderType: EgraphOrderLineItemType,
  @transient _services: PrintOrderLineItemTypeServices = AppConfig.instance[PrintOrderLineItemTypeServices]
)
  extends LineItemType[PrintOrder]
  with LineItemTypeEntityGetters[PrintOrderLineItemType]
  with HasTransientServices[PrintOrderLineItemTypeServices]
{

  override lazy val _entity = services.findOrCreateEntity()
  override def id = _entity.id

  override def lineItems(resolved: LineItems, unresolved: LineItemTypes): Option[LineItems] = None

  override def toJson = ""
}

object PrintOrderLineItemType {
  def codeType = CheckoutCodeType.PrintOrder
  def nature = LineItemNature.Product
}

case class PrintOrderLineItemTypeServices @Inject() (
  schema: Schema
) extends InsertsAndUpdates[LineItemTypeEntity] {
  import org.squeryl.PrimitiveTypeMode._

  override protected def table = schema.lineItemTypes

  def findOrCreateEntity() = {
    val maybeEntity = from(table)( entity =>
      where(entity._codeType === PrintOrderLineItemType.codeType.name)
      select(entity)
    ).headOption

    maybeEntity getOrElse { insert(baseEntity) }
  }

  private def baseEntity = LineItemTypeEntity(
    "Order of Framed Print",
    PrintOrderLineItemType.nature,
    PrintOrderLineItemType.codeType
  )
}
