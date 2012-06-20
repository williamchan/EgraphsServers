package frontend.formatting

import java.util
import java.text.SimpleDateFormat

object DateFormatting {
  object Conversions {
    class FormattableDate(date: util.Date) {
      def formatDayAsPlainLanguage = {
        new SimpleDateFormat("MMMM dd, yyyy").format(date)
      }
    }

    implicit def dateToFormattedDate(date: util.Date): FormattableDate = {
      new FormattableDate(date)
    }
  }
}
