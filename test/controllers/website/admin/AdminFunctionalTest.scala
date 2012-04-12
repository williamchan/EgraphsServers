package controllers.website.admin

import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.runScenarios
import scala.collection.JavaConversions._
import play.mvc.Http.Response

trait AdminFunctionalTest extends FunctionalTest {

  def createAdmin() {
    runScenarios("Create-Admin")
  }

  def createAndLoginAsAdmin(): Response = {
    createAdmin()
    loginAsAdmin()
  }

  def loginAsAdmin(): Response = {
    val response = POST("/admin/login", Map[String, String]("email" -> "admin@egraphs.com","password" -> "derp"))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities", response)
    response
  }

  /**
   * Posts to the admin logout controller.
   *
   * HACK ALERT! Also needs to call FunctionalTest.clearCookies because there is no good way to manage session data
   * in FunctionalTest context. Logout functionality should be manually tested before every public release.
   */
  def logoutFromAdminConsole(): Response = {
    val response = POST("/admin/logout")
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
    clearCookies()
    response
  }

}
