package utils

import java.util.Date
import org.scalatest.matchers.ShouldMatchers
import org.joda.time.DateTimeConstants

/**
 * Provides ShouldMatcher support for Dates, with some additional utility functions.
 * Mostly just piggybacks on Long ShouldMatcher support.
 * Can only be mixed in by classes that also extend ShouldMatchers.
 *
 * Usage:
 * <code>
 *    new Date should be (aboutNow)
 *    new Date should be (now plusOrMinus 2.days)
 * </code>
 */
trait DateShouldMatchers { this: ShouldMatchers =>

  /**
   * Enriches Long with methods that return that Long value of different time
   * units in milliseconds.
   *
   * For example:
   * <code>
   *   10 milliseconds // 10
   *   10 seconds      // 10000
   *   // etc...
   * </code>
   */
  class TimeUnitCount(value: Long) {
    val milliseconds = value
    lazy val seconds = value * DateTimeConstants.MILLIS_PER_SECOND
    lazy val minutes = value * DateTimeConstants.MILLIS_PER_MINUTE
    lazy val hours = value * DateTimeConstants.MILLIS_PER_HOUR
    lazy val days = value * DateTimeConstants.MILLIS_PER_DAY
  }

  /**
   * Returns a reasonable window of time around now (about half a second)
   *
   * Usage:
   * <code>
   *    new Date should be (aboutNow)
   * </code>
   */
  def aboutNow: LongTolerance = {
    (now plusOrMinus (500 milliseconds))
  }

  /**
   * Enriches Int to give it time unit support.
   */
  implicit def convertIntToTimeUnitCount(value: Int): TimeUnitCount = {
    new TimeUnitCount(value)
  }

  /**
   * Piggybacks Date ShouldMatcher support onto long
   */
  implicit def convertDateToLongShouldWrapper(date: Date): LongShouldWrapper = {
    new LongShouldWrapper(date.getTime)
  }

  /**
   * Piggybacks Date ShouldMatcher support onto Long
   */
  implicit def convertDateToPlusOrMinusWrapper(date: Date): LongPlusOrMinusWrapper = {
    new LongPlusOrMinusWrapper(date.getTime)
  }

  /** Returns the current moment in time. A convenience method around <code>new Date</code> */
  def now: Date = {
    new Date
  }
}
