// TODO: PLAY20 MIGRATION: myyk - I think that we will mostly delete this file (or wholly) since this doesn't seem to be how you hook into 
//   Play2 in tests.
package utils

import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, Call}
import play.api.test.Helpers._

import services.http.BasicAuth

//import java.util.Properties
//import models.Account
//import play.api.mvc.Http.Request
//import play.api.test.FunctionalTest
//
/**
 * Common functionality required when writing functional tests against
 * controller methods.
 */
object FunctionalTestUtils {
//  /**
//   * Makes an account identified by wchan83@egraphs.com/derp
//   */
//  def willChanAccount: Account = {
//    Account(email = "wchan83@egraphs.com").withPassword(TestData.defaultPassword).right.get
//  }
//
//  /**
//   * Makes an API request verified by the credentials from `willChanAccount`
//   */
  def willChanRequest: FakeRequest[AnyContent] = {
    val auth = BasicAuth.Credentials("wchan83@egraphs.com", TestData.defaultPassword)
    
    FakeRequest().withHeaders(auth.toHeader)
  }

  def requestWithCredentials(user: String, password: String): FakeRequest[AnyContent] = {
    val auth = BasicAuth.Credentials(user, password)

    FakeRequest().withHeaders(auth.toHeader)
  }
//
//  def createRequest(host: String = "www.egraphs.com", url: String = "/", secure: Boolean = false): Request = {
//    val request = FunctionalTest.newRequest()
//    request.host = host
//    request.url = url
//    request.secure = secure
//    request
//  }
//
//  def createProperties(propName: String, propValue: String): Properties = {
//    val playConfig = new Properties
//    playConfig.setProperty(propName, propValue)
//    playConfig
//  }
//
  def runScenarios(names: String*) {
    names.foreach { name =>
        runScenario(name)
    }
  }

  def runFreshScenarios(names: String*) {
    runScenario("clear")
    runScenarios(names: _*)
  }

  def runScenario(name: String) {
    val result = routeAndCall(FakeRequest(GET, "/test/scenarios/" + name)).get
    if (status(result) != OK) {
      throw new IllegalArgumentException("Unknown scenario name " + name)
    }
  }

  def runWillChanScenariosThroughOrder() {
    runFreshScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each",
      "Deliver-All-Orders-to-Celebrities"
    )
  }

  def routeName(call: Call): String = {
    call.method + " " + call.url
  }
}