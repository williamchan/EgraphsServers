package services.http

import play.mvc.Scope
import utils.EgraphsUnitTest

class SafePlayParamsTests extends EgraphsUnitTest {
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