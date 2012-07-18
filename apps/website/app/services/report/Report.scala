package services.report

import java.io.{FileOutputStream, File}
import services.Time

/**
 * Reports are helpful queries for the Operations team to get their work done.
 * See https://egraphs.atlassian.net/wiki/display/DEV/Reports for reference.
 */
trait Report {

  val reportName: String

  protected val newline = "\r\n"

  protected def csvLine(args: Any*): String = {
    (args.toList ::: List(newline)).mkString(",")
  }

  protected def csvFile(csv: StringBuilder, reportName: String = reportName): File = {
    val file = File.createTempFile(reportName + "-" + Time.toBlobstoreFormat(Time.now), "csv")
    file.deleteOnExit()
    val out = new FileOutputStream(file)
    out.write(csv.toString().getBytes)
    out.close()
    file
  }

}
