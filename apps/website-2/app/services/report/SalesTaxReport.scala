package services.report

import com.google.inject.Inject
import services.db.Schema
import org.squeryl.PrimitiveTypeMode._
import java.io.File

class SalesTaxReport @Inject()(schema: Schema) extends Report {

  override val reportName = "sales-tax-report"

  def report(): File = {
    val cashTransactions = from(schema.cashTransactions)(txn =>
      select(txn)
      orderBy (txn.id desc)
    )

    val headerLine = tsvLine(
      "id",
      "orderId",
      "printOrderId",
      "amount",
      "billingPostalCode",
      "cashTransactionType",
      "createdDate"
    )
    val tsv = new StringBuilder(headerLine)
    for (txn <- cashTransactions) {
      tsv.append(tsvLine(
        txn.id,
        txn.orderId.getOrElse(""),
        txn.printOrderId.getOrElse(""),
        txn.amountInCurrency,
        txn.billingPostalCode.getOrElse(""),
        txn.cashTransactionType.toString,
        formatDate(txn.created)
      )
      )
    }
    tsvFile(tsv)
  }
}
