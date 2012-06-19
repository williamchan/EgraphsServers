package utils

import java.util.Properties
import models.Account
import play.mvc.Http.Request
import play.test.FunctionalTest

/**
 * Common functionality required when writing functional tests against
 * controller methods.
 */
object FunctionalTestUtils {
  /**
   * Makes an account identified by wchan83@egraphs.com/derp
   */
  def willChanAccount: Account = {
    Account(email = "wchan83@egraphs.com").withPassword(TestData.defaultPassword).right.get
  }

  /**
   * Makes an API request verified by the credentials from `willChanAccount`
   */
  def willChanRequest: Request = {
    val req = FunctionalTest.newRequest()
    req.user = "wchan83@egraphs.com"
    req.password = TestData.defaultPassword

    req
  }

  def createRequest(host: String = "www.egraphs.com", url: String = "/", secure: Boolean = false): Request = {
    val request = FunctionalTest.newRequest()
    request.host = host
    request.url = url
    request.secure = secure
    request
  }

  def createProperties(propName: String, propValue: String): Properties = {
    val playConfig = new Properties
    playConfig.setProperty(propName, propValue)
    playConfig
  }

  def runScenarios(name: String*) {
    name.foreach {
      name =>
        runScenario(name)
    }
  }

  def runScenario(name: String) {
    val response = FunctionalTest.GET("/test/scenarios/" + name)
    if (response.status != 200) {
      throw new IllegalArgumentException("Unknown scenario name " + name)
    }
  }

  def runWillChanScenariosThroughOrder() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each",
      "Deliver-All-Orders-to-Celebrities"
    )
  }
}