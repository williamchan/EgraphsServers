package services.report

import com.google.inject.Inject
import services.db.Schema
import java.io.File
import org.squeryl.PrimitiveTypeMode._

class OrderReport @Inject()(schema: Schema) extends Report {

  override val reportName = "order-report"

  def report(): File = {
    import schema.{orders, customers, accounts, products, celebrities, egraphs}

    val orderViews = join(orders, customers, accounts, customers, products, celebrities, egraphs.leftOuter)(
      (order, buyer, buyerAccount, recipient, product, celebrity, egraph) =>
        select(order, buyer, buyerAccount, recipient, product, celebrity, egraph)
          orderBy (order.id asc)
          on(order.buyerId === buyer.id, buyer.id === buyerAccount.customerId, order.recipientId === recipient.id, order.productId === product.id, product.celebrityId === celebrity.id, order.id === egraph.map(_.orderId))
    )

    val headerLine = tsvLine(
      "orderId",
      "amount",
      "paymentStatus",
      "reviewStatus",
      "ordertimePST",
      "orderdatePST",
      "expectedDate",
      "productId",
      "celebrityId",
      "celebrityName",
      "buyerId",
      "buyerName",
      "buyerEmail",
      "recipientId",
      "recipientName",
      "candidateEgraphid",
      "candidateEgraphState",
      "candidateEgraphSignedAtPST"
    )

    val tsv = new StringBuilder(headerLine)
    for (orderView <- orderViews) {
      val order = orderView._1
      val buyer = orderView._2
      val buyerAccount = orderView._3
      val recipient = orderView._4
      val product = orderView._5
      val celebrity = orderView._6
      val candidateEgraph = orderView._7
      tsv.append(tsvLine(
        order.id,
        order.amountPaidInCurrency,
        order._paymentStatus,
        order._reviewStatus,
        formatTime(order.created, "PST"),
        formatDate(order.created, "PST"),
        order.expectedDate,
        product.id,
        celebrity.id,
        celebrity.publicName,
        buyer.id,
        buyer.name,
        buyerAccount.email,
        recipient.id,
        recipient.name,
        candidateEgraph.map(_.id).getOrElse(""),
        candidateEgraph.map(_._egraphState).getOrElse(""),
        candidateEgraph.map(egraph => formatDate(egraph.getSignedAt, "PST")).getOrElse("")
      ))
    }
    tsvFile(tsv)
  }
}
