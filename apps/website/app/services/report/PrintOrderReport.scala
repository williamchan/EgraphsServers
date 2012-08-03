package services.report

import com.google.inject.Inject
import models._
import services.db.Schema
import org.squeryl.PrimitiveTypeMode._
import enums.EgraphState
import org.squeryl.Query
import java.io.File

class PrintOrderReport @Inject() (schema: Schema) extends Report {

  override val reportName = "physical-print-report"

  /**
   * Once the first batch of physical orders are fulfilled, this report should switch to selecting PrintOrder.
   */
  def report(): File = {
    import schema.{orders, egraphs, customers, accounts, products, celebrities}
    val printOrders: Query[(Order, Account, Celebrity, Egraph)] =
      from(orders, customers, accounts, products, celebrities, egraphs)((order, recipient, account, product, celebrity, egraph) =>
        where(
          // TODO: Once the first 49 print orders are fulfilled, rewrite to query PrintOrder table
          order.amountPaidInCurrency === BigDecimal(95) and
            recipient.id === order.recipientId and
            order.productId === product.id and
            account.customerId === recipient.id and
            product.celebrityId === celebrity.id and
            egraph.orderId === order.id and
            (egraph._egraphState in Seq(EgraphState.Published.name, EgraphState.ApprovedByAdmin.name))
        )
          select(order, account, celebrity, egraph)
          orderBy (order.id asc)
      )

    val headerLine = csvLine("orderId", "amount", "egraphid", "signedAt",
      "recipientName", "recipientEmail", "recipientAddress", "celebrityPublicName", "celebrityId", "egraphstate")
    val csv = new StringBuilder(headerLine)
    for (o <- printOrders) {
      val order = o._1
      val account = o._2
      val celebrity = o._3
      val egraph = o._4
      csv.append(csvLine(
        order.id,
        order.amountPaidInCurrency,
        egraph.id,
        egraph.signedAt.getOrElse(egraph.created),
        order.recipientName,
        account.email,
        "", // This will come from ShippingInfo once the initial print orders are fulfilled
        celebrity.publicName,
        celebrity.id,
        egraph._egraphState)
      )
    }
    csvFile(csv)
  }
}