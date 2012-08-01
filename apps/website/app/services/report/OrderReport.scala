package services.report

import com.google.inject.Inject
import services.db.Schema
import java.io.File
import org.squeryl.PrimitiveTypeMode._

class OrderReport @Inject()(schema: Schema) extends Report {

  override val reportName = "order-report"

  def report(): File = {
    import schema.{orders, customers, products, egraphs}

    val orderViews = join(orders, customers, customers, products, egraphs.leftOuter)(
      (order, buyer, recipient, product, egraph) =>
        select(order, buyer, recipient, product, egraph)
          orderBy (order.id asc)
          on(order.buyerId === buyer.id, order.recipientId === recipient.id, order.productId === product.id, order.id === egraph.map(_.orderId))
    )

    val headerLine = csvLine("orderid", "amount", "paymentstatus", "reviewstatus", "billingpostalcode", "ordercreated",
      "productid", "celebrityid", "buyerid", "buyername", "recipientid", "recipientname",
      "candidateegraphid", "candidateegraphstate")
    val csv = new StringBuilder(headerLine)
    for (orderView <- orderViews) {
      val order = orderView._1
      val buyer = orderView._2
      val recipient = orderView._3
      val product = orderView._4
      val candidateEgraph = orderView._5
      csv.append(csvLine(
        order.id,
        order.amountPaidInCurrency,
        order._paymentStatus,
        order._reviewStatus,
        order.billingPostalCode.getOrElse(""),
        order.created,
        product.id,
        product.celebrityId,
        buyer.id,
        buyer.name,
        recipient.id,
        recipient.name,
        candidateEgraph.map(_.id).getOrElse(""),
        candidateEgraph.map(_._egraphState).getOrElse("")
      ))
    }
    csvFile(csv)
  }
}
