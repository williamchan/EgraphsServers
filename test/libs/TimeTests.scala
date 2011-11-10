package libs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.sql.Date

class TimeTests extends UnitFlatSpec with ShouldMatchers {
  "toApiFormat" should "produce a properly formatted date string" in {
    Time.toApiFormat(new Date(1320961084580L)) should be ("2011-11-10 21:38:04.580")
  }
}