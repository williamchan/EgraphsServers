package services.report

import com.google.inject.Inject
import services.db.Schema
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import java.sql.Timestamp
import services.Time

class BoardReport @Inject()(schema: Schema) extends Report {

  override val reportName = "board-report"
  import schema.{orders, printOrders}

  def report(): File = {
    val tsv = new StringBuilder()
    val headerLine = tsvLine(
      "week",
      "# Orders",
      "Digital revenue",
      "Avr digital revenue",
      "# Prints",
      "Print revenue",
      "Total revenue",
      "# gifts",
      "% gifts",
      "% with prints"
    )
    tsv.append(headerLine)
    tsv.append(getData(minusWeeks = 1))
    tsv.append(getData(minusWeeks = 2))
    tsv.append(getData(minusWeeks = 3))
    tsv.append(getData(minusWeeks = 4))
    tsvFile(tsv)
  }

  private def getData(minusWeeks: Int = 1): String = {
    val weekStart = new Timestamp(Time.weekStart(minusWeeks).toDate.getTime)
    val weekEnd = new Timestamp(Time.weekStart(minusWeeks - 1).toDate.getTime)

    val ordersCountAndRevenue = from(orders)(order =>
      where(order.created between(weekStart, weekEnd))
        compute(countDistinct(order.id), sum(order.amountPaidInCurrency))
    ).single
    val printOrdersCountAndRevenue = from(printOrders)(printOrder =>
      where(printOrder.created between(weekStart, weekEnd))
        compute(countDistinct(printOrder.id), sum(printOrder.amountPaidInCurrency))
    ).single
    val giftsCount = from(orders)(order =>
      where(order.buyerId <> order.recipientId
        and (order.created between(weekStart, weekEnd)))
        compute (countDistinct(order.id))
    ).single

    val numOrders = ordersCountAndRevenue.measures._1.intValue()
    val revenueOrders = ordersCountAndRevenue.measures._2.getOrElse(BigDecimal(0)).doubleValue()
    val numPrintOrders = printOrdersCountAndRevenue.measures._1.intValue()
    val revenuePrintOrders = printOrdersCountAndRevenue.measures._2.getOrElse(BigDecimal(0)).doubleValue()
    val numGifts = giftsCount.measures.intValue()
    val avgDigitalRevenue = if (numOrders > 0) revenueOrders / numOrders else 0
    val giftRatio = if (numOrders > 0) numGifts.toDouble / numOrders * 100 else 0
    val printRatio = if (numOrders > 0) numPrintOrders.toDouble / numOrders * 100 else 0
    tsvLine(
      formatDate(weekStart) + " to " + formatDate(weekEnd),
      numOrders,
      revenueOrders,
      avgDigitalRevenue,
      numPrintOrders,
      revenuePrintOrders,
      revenueOrders + revenuePrintOrders,
      numGifts,
      giftRatio,
      printRatio
    )
  }

}
