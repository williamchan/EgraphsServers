package controllers.website.admin

import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, runScenarios}

class PostAdminLoginEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  @Test
  def testEmailAndPasswordValidation() {
    val response = POST("/admin/login", getPostStrParams(email = "", password = ""))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }

  @Test
  def testAuthenticationValidation() {
    val response = POST("/admin/login", getPostStrParams())
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }

  @Test
  def testSuccessfulLogin() {
    runScenarios(
      "Create-Admin"
    )

    val response = POST("/admin/login", getPostStrParams(email = "admin@egraphs.com", password = "derp"))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities", response)
  }

  private def getPostStrParams(email: String = "idontexist@egraphs.com",
                               password: String = "derp"): Map[String, String] = {
    Map[String, String](
      "email" -> email,
      "password" -> password
    )
  }

}
