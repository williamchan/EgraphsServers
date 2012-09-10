package services.report

import java.io.{FileOutputStream, File}
import services.Time
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

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


  val timeFormatter = new SimpleDateFormat("HH:mm:ss")
  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  protected def formatTime(date: Date) = {
    timeFormatter.setTimeZone(TimeZone.getTimeZone("PST"))
    timeFormatter.format(date)
  }

  protected def formatDate(date: Date) = {
    dateFormatter.setTimeZone(TimeZone.getTimeZone("PST"))
    dateFormatter.format(date)
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
