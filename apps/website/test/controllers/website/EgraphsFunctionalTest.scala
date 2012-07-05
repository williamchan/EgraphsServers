package controllers.website

import java.net.URLDecoder
import play.mvc.Http.Response
import play.test.FunctionalTest
import FunctionalTest._
import models.Account
import org.junit.After
import scala.collection.JavaConversions._
import services.Utils
import utils.TestData
import org.junit.Assert._
import services.http.forms.CustomerLoginForm

trait EgraphsFunctionalTest extends FunctionalTest with CleanDatabaseAfterEachTest {

  def getPlayFlashCookie(response: Response): String = {
    URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
  }

  def getPlayErrorCookie(response: Response): String = {
    URLDecoder.decode(response.cookies.get("PLAY_ERRORS").value, "US-ASCII")
  }

  def login(account: Account, password: String = TestData.defaultPassword): Response = {
    import CustomerLoginForm.Fields

    clearCookies()
    val response = POST(
      Utils.lookupUrl("WebsiteControllers.postLogin"),
      Map[String, String](
        Fields.Email.name -> account.email,
        Fields.Password.name -> password
      )
    )
    assertStatus(302, response)
    assertFalse(getPlayFlashCookie(response).contains("error"))
    response
  }

  /**
   * Posts to the logout controller.
   * HACK ALERT! FunctionalTest.clearCookies is called because there is no good way to manage session data
   * in FunctionalTest context. Logout functionality should be manually tested before every public release.
   */
  def logout(): Response = {
    val response = POST(Utils.lookupUrl("WebsiteControllers.postLogout").url)
    assertStatus(302, response)
    clearCookies()
    response
  }
}

trait CleanDatabaseAfterEachTest { this: FunctionalTest =>
  @After
  def cleanUpDatabase() {
    FunctionalTest.GET("/test/scenarios/clear")
  }
}
