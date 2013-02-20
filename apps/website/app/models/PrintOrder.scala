package models

import com.google.inject.Inject
import enums.EgraphState._
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{FilterOneTable, SavesWithLongKey, Schema, KeyedCaseClass}
import services.Finance.TypeConversions._
import org.joda.money.Money
import org.squeryl.Query
import services.print.{PrintManufacturingInfo, LandscapeFramedPrint}
import services.blobs.AccessPolicy

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
                      lineItemId: Option[Long] = None,
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
   * If an associated published or approved Egraph exists, generates a print-sized egraph image and returns its url.
   * If the image was previously generated and stored, then the url is returned.
   * @return url of print-sized image
   */
  def getPngUrl: Option[String] = {
    services.egraphStore.findByOrder(orderId, services.egraphQueryFilters.publishedOrApproved).headOption.map {egraph =>
      egraph.getEgraphImage(LandscapeFramedPrint.targetEgraphWidth, ignoreMasterWidth=false)
        .asPng
        .getSavedUrl(AccessPolicy.Public)
    }
  }

  /**
   * If an associated published or approved Egraph exists, generates an assembled image for a framed print and returns
   * its url. If the image was previously generated and stored, then the url is returned. Also, printing data as
   * required by our printing partner is returned as comma-separated values.
   * @return url of framed print image and CSV string according to our printing partner's spec
   */
  def getFramedPrintImageData: Option[(String, String)] = {
    services.egraphStore.findByOrder(orderId, services.egraphQueryFilters.publishedOrApproved).headOption.map {egraph =>
      val thisOrder = egraph.order
      val imageUrl = egraph.getFramedPrintImageUrl
      val csv = PrintManufacturingInfo.toCSVLine(buyerEmail = thisOrder.buyer.account.email,
        shippingAddress = shippingAddress,
        partnerPhotoFile = egraph.framedPrintFilename)
      (imageUrl, csv)
    }
  }

  /**
   * @return url to a read-to-print image of the certificate of authenticity
   */
  def getStandaloneCertificateUrl: Option[String] = {
    services.egraphStore.findByOrder(orderId, services.egraphQueryFilters.publishedOrApproved).headOption.map {egraph =>
      egraph.getStandaloneCertificateUrl
    }
  }

  def withShippingAddress(address: Address) = { this.copy(shippingAddress = address.streetAddressString) }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = PrintOrder.unapply(this)
}

object PrintOrder {
  val pricePerPrint = BigDecimal(45)
}

class PrintOrderStore @Inject() (schema: Schema) extends SavesWithLongKey[PrintOrder] with SavesCreatedUpdated[PrintOrder] {
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
        orderBy (printOrder.id asc)
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
        orderBy (printOrder.id asc)
        on(printOrder.orderId === order.id, order.id === egraph.orderId and (egraph._egraphState in Seq(ApprovedByAdmin.name, Published.name)))
    )
  }

  def findByLineItemId(id: Long): Query[PrintOrder] = {
    table.where(printOrder => printOrder.lineItemId === Some(id))
  }

  //
  // SavesWithLongKey[PrintOrder] methods
  //
  override val table = schema.printOrders


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

