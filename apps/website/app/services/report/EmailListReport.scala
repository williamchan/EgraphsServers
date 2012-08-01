package services.report

import com.google.inject.Inject
import services.db.Schema
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import java.io.File

class EmailListReport @Inject()(schema: Schema) extends Report {

  override val reportName = "notice-stars-report"

  def report(): File = {
    import schema.{accounts, customers}
    val subscriberEmails: Query[String] =
      from(accounts, customers)((account, customer) =>
        where(
          account.customerId === customer.id and
            customer.notice_stars === true
        )
          select (&(account.email))
      )

    val headerLine = csvLine("email")
    val csv = new StringBuilder(headerLine)
    for (email <- subscriberEmails) {
      csv.append(csvLine(email))
    }
    csvFile(csv)
  }

}
