package services.report

import com.google.inject.Inject
import java.io.File
import java.sql.Timestamp
import org.squeryl.dsl.GroupWithMeasures
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.db.Schema
import services.Time

class MonthlyCelebrityPrintOrderReport @Inject()(schema: Schema) extends Report {

  override val reportName = "monthly-celebrity-print-order-report"

  def report(): File = {
    import schema.{printOrders, orders, products, celebrities}
    val previousMonthStart = new Timestamp(Time.previousMonthStart.toDate.getTime)
    val currentMonthStart = new Timestamp(Time.currentMonthStart.toDate.getTime)
    val query: Query[GroupWithMeasures[Product2[Long /*productId*/ , String /*publicName*/ ], Product2[Long /*count*/ , Option[BigDecimal] /*sum*/ ]]] =
      from(printOrders, orders, products, celebrities)((printOrder, order, product, celebrity) =>
        where(printOrder.orderId === order.id and order.productId === product.id and product.celebrityId === celebrity.id
          and (order.created between(previousMonthStart, currentMonthStart))
        )
          groupBy(order.productId, celebrity.publicName)
          compute(countDistinct(printOrder.id), sum(printOrder.amountPaidInCurrency))
          orderBy (celebrity.publicName)
      )

    val timeRangeLine = tsvLine(previousMonthStart, currentMonthStart)
    val headerLine = tsvLine("productId", "celebrityPublicName", "numSold", "totalRevenue")
    val tsv = new StringBuilder(timeRangeLine).append(headerLine)
    for (row <- query) {
      tsv.append(tsvLine(
        row.key._1,
        row.key._2,
        row.measures._1,
        row.measures._2.getOrElse(0)
      ))
    }
    tsvFile(tsv)
  }
}
