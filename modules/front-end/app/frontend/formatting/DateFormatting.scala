package frontend.formatting

import java.util
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
      /** Formats the date thusly: "May 10, 2012." "September 4, 2012." Etc. */
      def formatDayAsPlainLanguage = {
        new SimpleDateFormat("MMMM dd, yyyy").format(date)
      }
    }

    implicit def dateToFormattedDate(date: util.Date): FormattableDate = {
      new FormattableDate(date)
    }
  }
}
