package services.http.filters

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.OK
import play.api.test.Helpers.status
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class FilterTests extends FlatSpec with ShouldMatchers with MockFactory {

  val notFound = NotFound("Boolean not provided but should have been.") // Note: this must be a val for assertions to work on equals

  /**
   * This simple implementation of a Filter allows us to filter strings that are representations of booleans.
   * This is just for testing Filter.
   */
  class TestableFilter extends Filter[String, Boolean] {
    override def filter(value: String): Either[Result, Boolean] = value match {
      case "true" => Right(true)
      case "false" => Right(false)
      case _ => Left(notFound)
    }
  }

  "filter" should "give the happy case on the right and sad on the left" in {
    // Setup
    val filter = new TestableFilter()

    filter.filter("Hey There!") should be(Left(notFound))
    filter.filter("true") should be(Right(true))
    filter.filter("false") should be(Right(false))
  }

  "apply" should "execute the code in an action from the actionFactory only if the requirement is found" in {
    // Setup
    val filter = new TestableFilter()

    val (request, resultFromAction, actionFactory) = setupMocks(1)

    // Test
    val action = filter("true")(actionFactory)
    val result = action(request)

    // Verify
    status(result) should be(OK)
    result should be(resultFromAction)
  }

  it should "not execute the code in an action from the actionFactory if the requirement is not found" in {
    // Setup
    val filter = new TestableFilter()

    val (request, resultFromAction, actionFactory) = setupMocks(0)

    // Test
    val action = filter("I should not pattern match")(actionFactory)
    val result = action(request)

    // Verify
    status(result) should be(NOT_FOUND)
    result should be(notFound)
  }

  it should "not execute the filter code while creating an action (but rather while evaluating it)" in {
    // Setup
    val filter = new Filter[String, Boolean] {
      override def filter(value: String): Either[Result, Boolean] = value match {
        case _ => throw new Exception("test should not run the filter code")
      }
    }

    // Test
    val action = filter("I should not pattern match") { someBoolean =>
      Action { Ok }
    }
  }

  // Setup mock actionFactory to return an action that would return a 200 OK with the work "true" in it
  private def setupMocks(expectedNumberOfCallsToActionFactory: Int): (Request[AnyContent], Result, Boolean => Action[AnyContent]) = {
    val request = FakeRequest()

    val resultFromAction = Ok(true.toString)
    val action = Action {
      resultFromAction
    }
    val actionFactory = mockFunction[Boolean, Action[AnyContent]]
    actionFactory expects (true) returning action repeat (expectedNumberOfCallsToActionFactory)

    (request, resultFromAction, actionFactory)
  }
}