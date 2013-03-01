package models.checkout

import Conversions._
import com.google.inject.Inject
import models.enums.{LineItemNature, CheckoutCodeType}
import models.{Order, PrintOrder}
import services.db.{InsertsAndUpdates, HasTransientServices, Schema}
import services.AppConfig

//
// Services
//
/**
 * Only inserts and updates item type entities because the PrintOrderLineItemType doesn't need insert or
 * update methods while it has a single possible entity.
 */
case class PrintOrderLineItemTypeServices @Inject() (schema: Schema) extends InsertsAndUpdates[LineItemTypeEntity] {
  import org.squeryl.PrimitiveTypeMode._
  import PrintOrderLineItemType._

  //
  // InsertsAndUpdates member
  //
  override protected def table = schema.lineItemTypes

  //
  // Helpers
  //
  def findOrCreateEntity() = existingEntity getOrElse insert(baseEntity)

  private def existingEntity = table.where(_._codeType === PrintOrderLineItemType.codeType.name).headOption
  private def baseEntity = LineItemTypeEntity("Order of Framed Print", nature, codeType)
}



//
// Model
//
/**
 * LineItemType for a print order. Currently (CE-13) has a singular persisted entity, rather than an entity for each
 * product or celebrity or something more granular.
 */
case class PrintOrderLineItemType(
  forOrder: Order = Order(),
  address: Option[String] = None,
  @transient _services: PrintOrderLineItemTypeServices = AppConfig.instance[PrintOrderLineItemTypeServices]
)
  extends LineItemType[PrintOrder]
  with LineItemTypeEntityGetters[PrintOrderLineItemType]
  with HasTransientServices[PrintOrderLineItemTypeServices]
{
  //
  // HasLineItemTypeEntity member
  //
  override lazy val _entity = services.findOrCreateEntity()

  //
  // LineItemType members
  //
  override def id = _entity.id

  /** Return no line items because this lineitem generation is handled in EgraphOrderLineItemType */
  override def lineItems(resolved: LineItems = Nil, unresolved: LineItemTypes = Nil)
  : Option[Seq[PrintOrderLineItem]] = Some {
    Nil
  }

  //
  // Helpers
  //
  def printOrder = PrintOrder(
    orderId = forOrder.id,
    shippingAddress = address getOrElse ""
  )

  // TODO: define a SubLineItemType trait to make this required and handle overriding lineItems appropriately
  def getLineItem = PrintOrderLineItem(this, printOrder)
}



//
// Companion
//
object PrintOrderLineItemType {
  def codeType = CheckoutCodeType.PrintOrder
  def nature = LineItemNature.Product
}
