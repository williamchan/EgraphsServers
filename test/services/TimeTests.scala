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
}