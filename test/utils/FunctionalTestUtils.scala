package utils

import models.Account
import play.mvc.Http.Request
import play.test.FunctionalTest
import org.junit.After

/**
 * Common functionality required when writing functional tests against
 * controller methods.
 */
object FunctionalTestUtils {
  /**
   * Makes an account identified by wchan83@gmail.com/herp
   */
  def willChanAccount: Account = {
    Account(email = "wchan83@gmail.com").withPassword("herp").right.get
  }

  /**
   * Makes an API request verified by the credentials from `willChanAccount`
   */
  def willChanRequest: Request = {
    val req = FunctionalTest.newRequest()
    req.user = "wchan83@gmail.com"
    req.password = "herp"

    req
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

  trait CleanDatabaseAfterEachTest { this: FunctionalTest =>
    @After
    def cleanUpDatabase() {
      FunctionalTest.GET("/test/scenarios/clear")
    }
  }
}