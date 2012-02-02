package services.http

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.mvc.Scope
import play.mvc.Http.Request

class OptionParamsTests extends UnitFlatSpec
  with ShouldMatchers
  with Mockito
{
  import OptionParams.Conversions._

  "getOption" should "turn empty string parameters into None" in {
    val params = mock[Scope.Params]

    params.get("name") returns ""

    params.getOption("name") should be (None)
  }

  it should "turn null string parameters into None" in {
    val params = mock[Scope.Params]

    params.get("name") returns null

    params.getOption("name") should be (None)
  }

  it should "turn string parameters with values into Some(theValue)" in {
    val params = mock[Scope.Params]

    params.get("name") returns "value"

    params.getOption("name") should be (Some("value"))
  }
}