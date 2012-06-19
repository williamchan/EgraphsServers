package services.http

import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import play.mvc.Scope
import play.test.UnitFlatSpec

class SafePlayParamsTests extends UnitFlatSpec
  with ShouldMatchers
  with Mockito
{
  import SafePlayParams.Conversions._

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

  "getLongOption" should "turn empty Long parameters into None" in {
    val params = mock[Scope.Params]

    params.get("name") returns ""

    params.getLongOption("name") should be (None)
  }

  "getLongOption" should "turn malformed parameters into None" in {
    val params = mock[Scope.Params]

    params.get("name") returns "herp"

    params.getLongOption("name") should be (None)
  }

  "getLongOption" should "turn Long parameters into Some(Long)" in {
    val params = mock[Scope.Params]

    params.get("name") returns "1"

    params.getLongOption("name") should be (Some(1L))
  }
}