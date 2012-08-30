package models

import com.google.inject.Inject
import enums.EgraphState._
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{FilterOneTable, SavesWithLongKey, Schema, KeyedCaseClass}
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

  /**
   * Generates a print-sized png from an associated published or approved Egraph, if one exists.
   * @return url of generated image, if it was generated
   */
  def generatePng(): Option[String] = {
    val order = services.orderStore.get(orderId)
    services.egraphStore.findByOrder(orderId, services.egraphQueryFilters.publishedOrApproved).headOption.map {egraph =>
      val product = order.product
      val rawSignedImage = egraph.image(product.photoImage)
      // targetWidth is either the default width, or the width of the master if necessary to avoid upscaling
      val targetWidth = {
        val masterWidth = product.photoImage.getWidth
        if (masterWidth < PrintOrder.defaultPngWidth) masterWidth else PrintOrder.defaultPngWidth
      }
      val image = rawSignedImage
        .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
        .scaledToWidth(targetWidth)
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
  val defaultPngWidth = 2446         // 2446 seems to work well for physical prints
}

class PrintOrderStore @Inject() (schema: Schema) extends SavesWithLongKey[PrintOrder] with SavesCreatedUpdated[Long,PrintOrder] {
  import org.squeryl.PrimitiveTypeMode._

  def findByOrderId(orderId: Long): List[PrintOrder] = {
    from(schema.printOrders)(printOrder =>
      where(printOrder.orderId === Some(orderId))
        select (printOrder)
    ).toList
  }

  def findByFilter(filters: FilterOneTable[PrintOrder]*): Query[(PrintOrder, Order, Option[Egraph])] = {
    join(schema.orders, schema.printOrders, schema.egraphs.leftOuter)((order, printOrder, egraph) =>
      where(FilterOneTable.reduceFilters(filters, printOrder))
        select(printOrder, order, egraph)
        orderBy (printOrder.created asc)
        on(printOrder.orderId === order.id, order.id === egraph.map(_.orderId) and (egraph.map(_._egraphState) in Seq(ApprovedByAdmin.name, Published.name)))
    )
  }

  /**
   * Returns a list of PrintOrders that have egraphs but for which high-res PNGs have not yet
   * been generated for creating the physical collateral.
   */
  def findHasEgraphButLacksPng(): Query[(PrintOrder, Order, Option[Egraph])] = {
    join(schema.orders, schema.printOrders, schema.egraphs)((order, printOrder, egraph) =>
      where(printOrder.isFulfilled === false and printOrder.pngUrl.isNull)
        select(printOrder, order, Option(egraph))
        orderBy (printOrder.created asc)
        on(printOrder.orderId === order.id, order.id === egraph.orderId and (egraph._egraphState in Seq(ApprovedByAdmin.name, Published.name)))
    )
  }

  //
  // SavesWithLongKey[PrintOrder] methods
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
  // SavesCreatedUpdated[Long,PrintOrder] methods
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

  /** Matches PrintOrders that already have generated high-res PNGs */
  def hasPng: FilterOneTable[PrintOrder] = {
    new FilterOneTable[PrintOrder] {
      override def test(printOrder: PrintOrder) = {
        (printOrder.isFulfilled === false and printOrder.pngUrl.isNotNull)
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

