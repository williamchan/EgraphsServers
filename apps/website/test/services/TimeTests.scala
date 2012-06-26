package services

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.sql.Date

class TimeTests extends UnitFlatSpec with ShouldMatchers {

  "API format converters" should "produce correct values" in {
    val dateMillis = 1320961084580L
    val dateString = "2011-11-10 21:38:04.580"

    // API format
    Time.toApiFormat(new Date(dateMillis)) should be (dateString)
    Time.fromApiFormat(dateString).getTime should be (dateMillis)
  }

  "Blobstore format converters" should "produce correct values" in {
    val dateMillis = 1320961084580L
    val dateString = "20111110213804580"

    Time.toBlobstoreFormat(new Date(dateMillis)) should be (dateString)
  }

  "stopwatch" should "report accurate times in seconds" in {
    val (result, duration) = Time.stopwatch {
      Thread.sleep(200)

      1
    }

    result should be (1)
    duration should be >= (0.200)
  }

  "second duration conversions" should "be correct" in {
    import Time.IntsToSeconds._

    1.second should be (1)
    1.minute should be (60)
    1.hour should be (3600)
    1.day should be (3600 * 24)

    1.seconds should be (1.second)
    1.minutes should be (1.minute)
    1.hours should be (1.hour)
    1.days should be (1.day)

    2.seconds should be (2)
  }
}