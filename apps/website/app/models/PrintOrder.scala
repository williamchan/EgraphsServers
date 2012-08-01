package models

import com.google.inject.Inject
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{FilterOneTable, Saves, Schema, KeyedCaseClass}
import services.Finance.TypeConversions._
import org.joda.money.Money
import services.blobs.AccessPolicy
import org.squeryl.Query

case class PrintOrderServices @Inject() (store: PrintOrderStore,
                                         orderStore: OrderStore,
                                         egraphStore: EgraphStore,
                                         egraphQueryFilters: EgraphQueryFilters)

/**
 * Persistent entity representing the Orders made upon Products of our service
 */
case class PrintOrder(id: Long = 0,
                      orderId: Long = 0,
                      shippingAddress: String = "",
                      quantity: Int = 1,
                      isFulfilled: Boolean = false,
                      amountPaidInCurrency: BigDecimal = PrintOrder.pricePerPrint,
                      pngUrl: Option[String] = None,
                      created: Timestamp = Time.defaultTimestamp,
                      updated: Timestamp = Time.defaultTimestamp,
                      services: PrintOrderServices = AppConfig.instance[PrintOrderServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated
{
  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): PrintOrder = {
    services.store.save(this)
  }

  def amountPaid: Money = {
    amountPaidInCurrency.toMoney()
  }

  def generatePng(): Option[String] = {
    val width = 2446 // width that seems to work for physical prints
    val order = services.orderStore.get(orderId)
    services.egraphStore.findByOrder(orderId, services.egraphQueryFilters.publishedOrApproved).headOption.map {egraph =>
      val product = order.product
      val rawSignedImage = egraph.image(product.photoImage)
      val image = rawSignedImage
        .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
        .scaledToWidth(width)
      image.rasterized.getSavedUrl(AccessPolicy.Public)
    }
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = PrintOrder.unapply(this)
}

object PrintOrder {
  val pricePerPrint = BigDecimal(45)
}

class PrintOrderStore @Inject() (schema: Schema) extends Saves[PrintOrder] with SavesCreatedUpdated[PrintOrder] {
  import org.squeryl.PrimitiveTypeMode._

  def findByFilter(filters: FilterOneTable[PrintOrder]*): Query[(PrintOrder, Order)] = {
    from(schema.printOrders, schema.orders)((printOrder, order) =>
      where(
        order.id === printOrder.orderId and
        FilterOneTable.reduceFilters(filters, printOrder)
      )
        select (printOrder, order)
        orderBy (printOrder.created asc)
    )
  }

  //
  // Saves[PrintOrder] methods
  //
  override val table = schema.printOrders

  override def defineUpdate(theOld: PrintOrder, theNew: PrintOrder) = {
    updateIs(
      theOld.orderId := theNew.orderId,
      theOld.shippingAddress := theNew.shippingAddress,
      theOld.quantity := theNew.quantity,
      theOld.isFulfilled := theNew.isFulfilled,
      theOld.amountPaidInCurrency := theNew.amountPaidInCurrency,
      theOld.pngUrl := theNew.pngUrl,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }
  //
  // SavesCreatedUpdated[PrintOrder] methods
  //
  override def withCreatedUpdated(toUpdate: PrintOrder, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

class PrintOrderQueryFilters @Inject() (schema: Schema) {
  import org.squeryl.PrimitiveTypeMode._

  def fulfilled: FilterOneTable[PrintOrder] = {
    new FilterOneTable[PrintOrder] {
      override def test(printOrder: PrintOrder) = {
        (printOrder.isFulfilled === true)
      }
    }
  }

  def unfulfilled: FilterOneTable[PrintOrder] = {
    new FilterOneTable[PrintOrder] {
      override def test(printOrder: PrintOrder) = {
        (printOrder.isFulfilled === false)
      }
    }
  }
}

