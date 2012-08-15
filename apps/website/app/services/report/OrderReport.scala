package services.report

import com.google.inject.Inject
import services.db.Schema
import java.io.File
import org.squeryl.PrimitiveTypeMode._
import models.enums.EgraphState
import java.text.SimpleDateFormat
import java.util.TimeZone

class OrderReport @Inject()(schema: Schema) extends Report {

  override val reportName = "order-report"


  def report(): File = {
    import schema.{orders, customers, products, celebrities, egraphs}

    val orderViews = join(orders, customers, customers, products, celebrities, egraphs.leftOuter)(
      (order, buyer, recipient, product, celebrity, egraph) =>
        where(not(egraph.map(_._egraphState) === Some(EgraphState.RejectedByAdmin.name)))
          select(order, buyer, recipient, product, celebrity, egraph)
          orderBy (order.id asc)
          on(order.buyerId === buyer.id, order.recipientId === recipient.id, order.productId === product.id, product.celebrityId === celebrity.id, order.id === egraph.map(_.orderId))
    )

    val headerLine = tsvLine(
      "orderid",
      "amount",
      "paymentstatus",
      "reviewstatus",
      "ordertimePST",
      "orderdatePST",
      "expectedDate",
      "productid",
      "celebrityid",
      "celebrityName",
      "buyerid",
      "buyername",
      "recipientid",
      "recipientname",
      "candidateegraphid",
      "candidateegraphstate"
    )
    // TODO wow this java library sucks
    val timeFormatter = new SimpleDateFormat("HH:mm:ss")
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    timeFormatter.setTimeZone(TimeZone.getTimeZone("PST"))
    dateFormatter.setTimeZone(TimeZone.getTimeZone("PST"))
    val tsv = new StringBuilder(headerLine)
    for (orderView <- orderViews) {
      val order = orderView._1
      val buyer = orderView._2
      val recipient = orderView._3
      val product = orderView._4
      val celebrity = orderView._5
      val candidateEgraph = orderView._6
      tsv.append(tsvLine(
        order.id,
        order.amountPaidInCurrency,
        order._paymentStatus,
        order._reviewStatus,
        timeFormatter.format(order.created),
        dateFormatter.format(order.created),
        order.expectedDate.getOrElse(""),
        product.id,
        celebrity.id,
        celebrity.publicName,
        buyer.id,
        buyer.name,
        recipient.id,
        recipient.name,
        candidateEgraph.map(_.id).getOrElse(""),
        candidateEgraph.map(_._egraphState).getOrElse("")
      ))
    }
    tsvFile(tsv)
  }
}
