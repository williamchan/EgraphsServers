package services

import java.sql.Timestamp
import java.util.{TimeZone, Date}
import java.text.{DateFormat, SimpleDateFormat}
import org.joda.time.{DateTimeConstants, DateMidnight, DateTime}
import play.api.libs.json._

/**
 * Convenience methods for dealing with time
 */
object Time {

  val millisInDay = DateTimeConstants.MILLIS_PER_DAY
  
  def ipadDateFormat = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") 
  }
  
  def ISO8601DatePattern = {
    "'ISO8601:'yyyy-MM-dd'T'HH:mm:ssZ"
  }

  /** What should be the default timestamp for entities. Occurs at epoch 0 */
  def defaultTimestamp: Timestamp = {
    new Timestamp(0L)
  }

  /** The current time */
  def now:Timestamp = {
    new Timestamp(new Date().getTime)
  }

  def today:Date = {
    DateTime.now().toDate
  }

  /**
   * @return the very start of the current month
   */
  def currentMonthStart: DateTime = new DateMidnight().withDayOfMonth(1).toDateTime

  /**
   * @return the very start of the current month
   */
  def weekStart(minusWeeks: Int = 0): DateTime = new DateMidnight().withDayOfWeek(DateTimeConstants.SUNDAY).minusWeeks(minusWeeks).toDateTime

  /**
   * @return the very start of the previous month
   */
  def previousMonthStart: DateTime = currentMonthStart.minusMonths(1)

  def timestamp(dateStr: String, dateFormat: DateFormat): Option[Timestamp] = {
    if (dateStr.isEmpty) {
      None
    } else {
      val date = dateFormat.parse(dateStr)
      Some(new Timestamp(date.getTime))
    }
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
   * Transforms the provided API-formatted date string into a Timestamp, assuming that
   * it adheres to the API format described in toApiFormat.
   */
  def fromApiFormat(dateString: String): Timestamp = {
    new Timestamp(apiDateFormat.parse(dateString).getTime)
  }

  /**
   * This will let you more easily write Dates to your json objects
   * using the api formatting.
   */
  implicit object ApiDateFormat extends Format[Timestamp] {
    def writes(timestamp: Timestamp): JsValue = {
      JsString(toApiFormat(timestamp))
    }

    def reads(json: JsValue): JsResult[Timestamp] = {
      JsSuccess {
        Time.fromApiFormat(json.as[String])
      }
    }
  }

  /**
   * Renders the provided date in the format used on the blobstore. That is
   * YearMonthDayHourMinuteSecondMillisecond.
   *
   * For example: Erem's moment of birth on May 10, 1983, 10:45PM and 451 milliseconds
   * would be rendered: 19830510204500451
   */
  def toBlobstoreFormat(date: Date) = {
    blobstoreDateFormat.format(date)
  }

  /** General Mountain Time / UTC Time Zone. */
  def GMT: TimeZone = {
    TimeZone.getTimeZone("GMT")
  }

  /**
   * Times how long an operation takes to occur in seconds. Returns a tuple of the operation's
   * result and the time it took to complete.
   *
   * @param operation the operation to execute and time
   * @tparam A return type of the operation
   * @return (operationReturnValue, durationInSeconds)
   */
  def stopwatch[A](operation: => A): (A, Double) = {
    val start = System.currentTimeMillis()
    val result = operation
    val stop = System.currentTimeMillis()

    (result, (stop - start).toDouble / 1000)
  }

  //
  // Private members
  //
  /** Provides a DateFormat for working with API-formatted date Strings. */
  private def apiDateFormat: DateFormat = {
    dateFormatInTimeZone("yyyy-MM-dd HH:mm:ss.SSS", GMT)
  }

  /** Provides a DateFormat for working with date strings meant for use as keys in the blobstore */
  private def blobstoreDateFormat: DateFormat = {
    dateFormatInTimeZone("yyyyMMddHHmmssSSS", GMT)
  }

  private def dateFormatInTimeZone(format: String, timezone: TimeZone): DateFormat = {
    val dateFormat = new SimpleDateFormat(format)
    dateFormat.setTimeZone(timezone)

    dateFormat
  }

  /**
   * Implicit conversions that turn Ints with various duration units into their
   * representative values in seconds.
   *
   * Usage:
   * {{{
   *   import Time.IntsToSeconds._
   *
   *   // The following are true
   *   1.second ==  1
   *   1.minute ==  60
   *   2.minutes == 120
   *   1.hour   ==  3600
   * }}}
   */
  object IntsToSeconds {
    class SecondDurations(int: Int) {
      //
      // Singulars
      //
      def second: Int = {
        int
      }

      def minute: Int = {
        DateTimeConstants.SECONDS_PER_MINUTE
      }

      def hour: Int = {
        DateTimeConstants.SECONDS_PER_HOUR
      }

      def day: Int = {
        DateTimeConstants.SECONDS_PER_DAY
      }

      //
      // Plurals
      //
      def days: Int = {
        day
      }

      def seconds: Int = {
        second
      }

      def minutes: Int = {
        minute
      }

      def hours: Int = {
        hour
      }
    }

    implicit def intsToSecondDurations(int: Int): SecondDurations = {
      new SecondDurations(int)
    }
  }
}
