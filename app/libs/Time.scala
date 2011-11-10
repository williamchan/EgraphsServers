package libs

import java.sql.Timestamp
import java.util.{TimeZone, Date}
import java.text.{DateFormat, SimpleDateFormat}

/**
 * Convenience methods for dealing with time
 */
object Time {
  /** What should be the default timestamp for entities. Occurs at epoch 0 */
  def defaultTimestamp: Timestamp = {
    new Timestamp(0L)
  }
  
  /** The current time */
  def now:Timestamp = {
    new Timestamp(new Date().getTime)
  }

  /**
   * Renders the provided date in the format used for our APIs.
   * That is, year-month-day hour:minute:second.millisecond
   *
   * For example: Erem's moment of birth on May 10, 1983, 10:45PM
   * and 451 milliseconds would be rendered: 1983-05-10 20:45:00.451
   */
  def toApiFormat(date: Date): String = {
    apiDateFormat.format(date)
  }

  /**
   * Transforms the provided API-formatted date string into a Timestasmp, assuming that
   * it adheres to the API format described in toApiFormat.
   */
  def fromApiFormat(dateString: String): Timestamp = {
    new Timestamp(apiDateFormat.parse(dateString).getTime)
  }

  /** Provides a DateFormat for working with API-formatted date Strings. */
  private def apiDateFormat: DateFormat = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    format.setTimeZone(TimeZone.getTimeZone("GMT"))

    format
  }
}