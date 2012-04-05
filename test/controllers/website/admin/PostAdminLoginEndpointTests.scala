package controllers.website.admin

import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest

class PostAdminLoginEndpointTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  @Test
  def testEmailAndPasswordValidation() {
    val response = POST("/admin/login", getPostStrParams(email = "", password = ""))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }

  @Test
  def testAuthenticationValidation() {
    val response = POST("/admin/login", getPostStrParams(email = "idontexist@egraphs.com", password = "herp"))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }

  @Test
  def testSuccessfulLogin() {
    val response = createAndLoginAsAdmin()
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities?", response)
  }

  private def getPostStrParams(email: String, password: String): Map[String, String] = {
    Map[String, String]("email" -> email, "password" -> password)
  }

}
