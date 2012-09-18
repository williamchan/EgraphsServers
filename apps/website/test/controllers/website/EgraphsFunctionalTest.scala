package controllers.website

import java.net.URLDecoder
import play.mvc.Http.Response
import play.test.FunctionalTest
import FunctionalTest._
import models.Account
import org.junit.Before
import scala.collection.JavaConversions._
import services.AppConfig
import utils.TestData
import org.junit.Assert._
import services.http.forms.CustomerLoginForm
import services.db.DBSession
import org.scalatest.matchers.ShouldMatchers
import controllers.WebsiteControllers

trait EgraphsFunctionalTest extends FunctionalTest
  with CleanDatabaseBeforeEachTest
  with ShouldMatchers
{

  protected val db = AppConfig.instance[DBSession]

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
      WebsiteControllers.reverse(WebsiteControllers.postLogin()),
      Map[String, String](
        Fields.Email -> account.email,
        Fields.Password -> password
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
    val response = POST(WebsiteControllers.reverse(WebsiteControllers.postLogin()).url)
    assertStatus(302, response)
    clearCookies()
    response
  }
}

trait CleanDatabaseBeforeEachTest { this: FunctionalTest =>
  @Before
  def cleanUpDatabase() {
    FunctionalTest.GET("/test/scenarios/clear")
  }
}
