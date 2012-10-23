package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import utils.EgraphsUnitTest
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class FilterTests extends EgraphsUnitTest {

  def notFound = NotFound("Boolean not provided but should have been.")

  /**
   * This simple implementation of a Filter allows us to filter strings that are representations of booleans.
   * This is just for testing Filter.
   */
  class TestableFilter extends Filter[String, Boolean] {
    def filter(value: String): Either[Result, Boolean] = value match {
      case "true" => Right(true)
      case "false" => Right(false)
      case _ => Left(notFound)
    }
  }

  "filter" should "should give the happy case on the right and sad on the left" in {
    // Setup
    val filter = new TestableFilter()

    filter.filter("Hey There!") should be(Left(notFound))
    filter.filter("true") should be(Right(true))
    filter.filter("false") should be(Right(false))
  }

  "apply" should "should execute the code in a action from the actionFactory only if the requirement is found" in {
    // Setup
    val filter = new TestableFilter()

    val request = FakeRequest()

    val expectedResult =  Ok(true.toString)
    val mockAction = mock[Action[AnyContent]]
    val actionFactory = mock[Boolean => Action[AnyContent]]
    actionFactory(any) returns mockAction
    mockAction(request) returns expectedResult

    val action = filter("true")(actionFactory)
    val result = action(request)

    result should be(expectedResult)
    filter.filter("true") should be(Right(true))
    filter.filter("false") should be(Right(false))
  }
}