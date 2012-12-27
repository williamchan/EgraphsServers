package services.report

import java.io.File
import services.{TempFile, Utils, Time}
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

  protected def formatTime(date: Date, timeZone: String = "GMT") = {
    timeFormatter.setTimeZone(TimeZone.getTimeZone(timeZone))
    timeFormatter.format(date)
  }

  protected def formatDate(date: Date, timeZone: String = "GMT") = {
    dateFormatter.setTimeZone(TimeZone.getTimeZone(timeZone))
    dateFormatter.format(date)
  }

  protected def tsvFile(tsv: StringBuilder, reportName: String = reportName): File = {
    val file = TempFile.named(reportName + "-" + Time.toBlobstoreFormat(Time.today) + ".tsv")
    Utils.saveToFile(tsv.toString().getBytes, file)
    file
  }

}
