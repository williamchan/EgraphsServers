package services.http.filters

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.OK
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.status
import play.api.test.FakeRequest
import utils.EgraphsUnitTest
import play.api.libs.json.Json
import play.api.mvc.Session
import play.api.mvc.Cookie
import play.api.mvc.Results

@RunWith(classOf[JUnitRunner])
class RequestFilterTests extends EgraphsUnitTest with MockFactory {

  val notFound = NotFound("Boolean not provided but should have been.") // Note: this must be a val for assertions to work on equals
  val validFormKey = "youAreAwesome"

  /**
   * This simple implementation of a RequestFilter allows us to filter strings that are representations of booleans.
   * This is just for testing RequestFilter this class would actually kind of suck for real usage.
   */
  class TestableRequestFilter extends Filter[String, Boolean] with RequestFilter[String, Boolean] {
    override def filter(value: String): Either[Result, Boolean] = value match {
      case "true" => Right(true)
      case "false" => Right(false)
      case _ => Left(notFound)
    }

    override def form: Form[String] = Form(single(validFormKey -> text))
  }

  "inRequest" should "should execute the code in an action from the actionFactory if the requirement is found in the request form url" in {
    val request = FakeRequest().withFormUrlEncodedBody((validFormKey, "true"))

    happyTest(request) { filter =>
      filter.inRequest()
    }
  }

  it should "should execute the code in an action from the actionFactory if the requirement is found in the request headers only" in {
    val json = Json.toJson(Map(validFormKey -> "true"))
    val request = FakeRequest().withJsonBody(json)

    happyTest(request) { filter =>
      filter.inRequest()
    }
  }

  it should "should not execute the code in an action from the actionFactory if the requirement is found in the cookie only" in {
    val cookie = Cookie(validFormKey, "true")
    val request = FakeRequest().withCookies(cookie)

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inRequest()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  it should "should not execute the code in an action from the actionFactory if the requirement should be found in the flash only" in new EgraphsTestApplication {
    val request = FakeRequest().withFlash((validFormKey, "true"))

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inRequest()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  it should "should not execute the code in an action from the actionFactory if the requirement should be found in the session only" in {
    val request = FakeRequest().withSession((validFormKey, "true"))

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inRequest()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  it should "should not use the actionFactory if the data in the request is not found" in {
    val request = FakeRequest() // no validFormKey param in request

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inRequest()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  it should "should not use the actionFactory if the data in the request does not meet the filter requirements" in {
    val request = FakeRequest().withFormUrlEncodedBody((validFormKey, " oh no :( "))

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inRequest()
      },
      verification = { result =>
        status(result) should be(NOT_FOUND)
        result should be(notFound)
      })
  }

  "inSession" should "should execute the code in an action from the actionFactory if the requirement is found in the session" in {
    val request = FakeRequest().withSession((validFormKey, "true"))

    happyTest(request) { filter =>
      filter.inSession()
    }
  }

  it should "should not execute the code in an action from the actionFactory if the requirement is not found in the session" in {
    val request = FakeRequest().withSession(("someOtherKey", "true"))

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inSession()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  "inRequestOrFlash" should "should execute the code in an action from the actionFactory if the requirement is found in the request form url" in {
    val request = FakeRequest().withFormUrlEncodedBody((validFormKey, "true"))

    happyTest(request) { filter =>
      filter.inFlashOrRequest()
    }
  }

  it should "should execute the code in an action from the actionFactory if the requirement is found in the flash" in {
    val request = FakeRequest().withFlash((validFormKey, "true"))

    happyTest(request) { filter =>
      filter.inFlashOrRequest()
    }
  }

  it should "should not execute the code in an action from the actionFactory if the requirement is not found in the flash or request" in {
    val request = FakeRequest().withFlash(("someOtherKey", "true"))

    filteredOutTest(request)(
      testOperation = { filter =>
        filter.inFlashOrRequest()
      },
      verification = { result =>
        status(result) should be(BAD_REQUEST)
      })
  }

  "badRequest" should "be able to be able to be overriden to Redirect" in {
    import play.api.mvc.Results.Redirect

    val filter = new TestableRequestFilter() {
      override protected def badRequest(formWithErrors: Form[String]): Result = Redirect("www.egraphs.com")
    }

    val cookie = Cookie(validFormKey, "true")
    val request = FakeRequest().withCookies(cookie)

    val (resultFromAction, actionFactory) = setupMocks(expectedNumberOfCallsToActionFactory = 0)

    val action = filter.inRequest()(actionFactory)
    val result = action(request)

    status(result) should be(SEE_OTHER) // SEE_OTHER is equivalent to code: 301 returned by Redirect
  }

  def happyTest(request: Request[AnyContent])(testOperation: RequestFilter[_, Boolean] => (Boolean => Action[AnyContent]) => Action[AnyContent]) = {
    test(expectedNumberOfCallsToActionFactory = 1, request)(
      testOperation = testOperation,
      verification = (actual, fromActionFactory) => {
        status(actual) should be(OK)
        actual should be(fromActionFactory)
      })
  }

  def filteredOutTest(request: Request[AnyContent])(testOperation: RequestFilter[_, Boolean] => (Boolean => Action[AnyContent]) => Action[AnyContent],
    verification: Result => Any) = {
    test(expectedNumberOfCallsToActionFactory = 0, request)(
      testOperation = testOperation,
      verification = (actualResult, _) => verification(actualResult))
  }

  def test(expectedNumberOfCallsToActionFactory: Int, request: Request[AnyContent]) // function of actual result and the result from action from action factory
  (testOperation: RequestFilter[_, Boolean] => (Boolean => Action[AnyContent]) => Action[AnyContent], // function from a filter to a function that takes the actionFactory to return an action
    verification: (Result, Result) => Any) = {
    val filter = new TestableRequestFilter()
    val (resultFromAction, actionFactory) = setupMocks(expectedNumberOfCallsToActionFactory)

    val action = testOperation(filter)(actionFactory)
    val result = action(request)
    verification(result, resultFromAction)
  }

  // Setup mock actionFactory to return an action that would return a 200 OK with the work "true" in it
  private def setupMocks(expectedNumberOfCallsToActionFactory: Int): (Result, Boolean => Action[AnyContent]) = {
    val resultFromAction = Ok(true.toString)
    val action = Action {
      resultFromAction
    }
    val actionFactory = mockFunction[Boolean, Action[AnyContent]]
    actionFactory expects (true) returning action repeat (expectedNumberOfCallsToActionFactory)

    (resultFromAction, actionFactory)
  }
}