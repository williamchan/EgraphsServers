package frontend.formatting

import scala.language.implicitConversions
import java.util
import java.util.TimeZone
import java.text.SimpleDateFormat

/**
 * Utilities for formatting dates on the front end.
 *
 * Usage:
 * {{{
 *   import DateFormatting.Conversions._
 *
 *   val now = new Date()
 *
 *   now.formatDayAsPlainLanguage == "May 10, 2012" // True
 * }}}
 */
object DateFormatting {
  object Conversions {
    class FormattableDate(date: util.Date) {

      /** Formats the date thusly: "May 10, 2012." "September 4, 2012." Etc.
       *  Pass this the timezone you'd like the date displayed in (i.e. "PST" or "GMT"). Defaults to "PST".
       *
       *  If passed a string that doesn't match the expected syntax of Timezone, "GMT" is used
       *  See: http://docs.oracle.com/javase/1.4.2/docs/api/java/util/TimeZone.html for details.
       */
      def formatDayAsPlainLanguage(timezone: String = "PST") = {

        val formatter = new SimpleDateFormat("MMMM dd, yyyy")
        formatter.setTimeZone(TimeZone.getTimeZone(timezone))

        formatter.format(date)
      }
    }

    implicit def dateToFormattedDate(date: util.Date): FormattableDate = {
      new FormattableDate(date)
    }
  }
}
