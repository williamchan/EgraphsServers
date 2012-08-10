package services.report

import java.io.{FileOutputStream, File}
import services.Time

/**
 * Reports are helpful queries for the Operations team to get their work done.
 * See https://egraphs.atlassian.net/wiki/display/DEV/Reports for reference.
 */
trait Report {

  protected val reportName: String

  protected val newline = "\r\n"

  protected def tsvLine(args: Any*): String = {
    (args.toList ::: List(newline)).mkString("\t")
  }

  protected def tsvFile(tsv: StringBuilder, reportName: String = reportName): File = {
    val file = File.createTempFile(reportName + "-" + Time.toBlobstoreFormat(Time.today), ".tsv")
    file.deleteOnExit()
    val out = new FileOutputStream(file)
    out.write(tsv.toString().getBytes)
    out.close()
    file
  }

}
