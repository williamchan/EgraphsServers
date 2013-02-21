package services.report

import scala.language.postfixOps
import com.google.inject.Inject
import services.db.Schema
import org.squeryl.PrimitiveTypeMode._
import java.io.File

class PrintOrderReport @Inject()(schema: Schema) extends Report {

  override val reportName = "physical-print-report"

  def report(): File = {
    import schema.{printOrders, orders, egraphs, customers, products, celebrities}
    val printOrderViews = join(printOrders, orders, customers, customers, products, celebrities, egraphs.leftOuter)(
      (printOrder, order, buyer, recipient, product, celebrity, egraph) =>
        select(printOrder, order, buyer, recipient, product, celebrity, egraph)
          orderBy (printOrder.id asc)
          on(printOrder.orderId === order.id, order.buyerId === buyer.id, order.recipientId === recipient.id, order.productId === product.id, product.celebrityId === celebrity.id, order.id === egraph.map(_.orderId))
    )

    val headerLine = tsvLine(
      "printOrderId",
      "isFulfilled",
      "pngUrl",
      "amount",
      "shippingAddress",
      "orderId",
      "orderCreatedDate",
      "orderExpectedDate",
      "productId",
      "celebrityId",
      "celebrityName",
      "buyerId",
      "buyerName",
      "recipientId",
      "recipientName",
      "candidateEgraphId",
      "candidateEgraphState",
      "candidateEgraphSignedAt"
    )
    val tsv = new StringBuilder(headerLine)
    for (printOrderView <- printOrderViews) {
      val printOrder = printOrderView._1
      val order = printOrderView._2
      val buyer = printOrderView._3
      val recipient = printOrderView._4
      val product = printOrderView._5
      val celebrity = printOrderView._6
      val candidateEgraph = printOrderView._7
      tsv.append(tsvLine(
        printOrder.id,
        printOrder.isFulfilled.toString,
        printOrder.pngUrl.getOrElse(""),
        printOrder.amountPaidInCurrency,
        printOrder.shippingAddress,
        order.id,
        formatDate(order.created),
        order.expectedDate,
        product.id,
        celebrity.id,
        celebrity.publicName,
        buyer.id,
        buyer.name,
        recipient.id,
        recipient.name,
        candidateEgraph.map(_.id).getOrElse(""),
        candidateEgraph.map(_._egraphState).getOrElse(""),
        candidateEgraph.map(_.getSignedAt).getOrElse("")
      )
      )
    }
    tsvFile(tsv)
  }
}
