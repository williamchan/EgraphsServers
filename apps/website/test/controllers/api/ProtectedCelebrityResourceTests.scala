package controllers.api

import utils.{EgraphsUnitTest, TestConstants, TestData}
import play.api.test.Helpers._
import play.api.mvc.{AnyContent, Call}
import utils.FunctionalTestUtils.{runFreshScenarios, routeName, requestWithCredentials}
import play.api.test.FakeRequest
import services.AppConfig
import services.http.BasicAuth
import services.db.{DBSession, TransactionSerializable}

trait ProtectedCelebrityResourceTests { this: EgraphsUnitTest =>
  protected def routeUnderTest: Call
  protected def validRequestBodyAndQueryString: Option[FakeRequest[AnyContent]] = None

  aBasicAuthProtectedCelebApiResource should "forbid requests with the wrong password for a valid celebrity account" in new EgraphsTestApplication {
    runFreshScenarios("Will-Chan-is-a-celebrity")

    // Assemble the request
    executingRequestWithCredentials("wchan83@egraphs.com", "wrong") should be (FORBIDDEN)
  }

  it should "forbid requests with the correct password for non-celebrity account" in new EgraphsTestApplication {
    val password = "herp derp derpson"
    val dbSession = AppConfig.instance[DBSession]

    val account = dbSession.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()      

      customer.account.withPassword(password).right.get.save()
    }

    executingRequestWithCredentials(account.email, password) should be (FORBIDDEN)
  }

  private def executingRequestWithCredentials(username: String, password: String): Int = {
    val auth = BasicAuth.Credentials(username, password)

    val requestBodyAndQuery = validRequestBodyAndQueryString.getOrElse(FakeRequest())

    val request = requestBodyAndQuery
      .copy(method=routeUnderTest.method, uri=routeUnderTest.url)
      .withHeaders(auth.toHeader)

    // Execute the request
    val Some(result) = routeAndCall(request)
    status(result)
  }

  private def aBasicAuthProtectedCelebApiResource = routeName(routeUnderTest) + ", as a basic-auth protected celebrity API resource,"
}
