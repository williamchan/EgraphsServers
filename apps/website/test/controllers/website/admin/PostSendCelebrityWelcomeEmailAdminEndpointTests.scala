package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import models.enums.PublishedStatus
import models.CelebrityStore
import services.db.{TransactionSerializable, DBSession}
import play.mvc.Http.Response

class PostSendCelebrityWelcomeEmail extends AdminFunctionalTest {
  
  @Test
  def testPostCelebritySendWelcomeEmail() {
    createAndLoginAsAdmin()
    createCeleb()
    val response = POST("/admin/celebrities/1/sendEmail", getPostStrParams("derp@derp.com"))
    assertStatus(302, response)
    assertEquals("[/admin/celebrities/1]", response.headers("Location").toString)
  }

  @Test
  def testPostCelebritySendWelcomeEmailNoCeleb() {
    createAndLoginAsAdmin()
    val response = POST("/admin/celebrities/1/sendEmail", getPostStrParams("derp@derp.com"))
    assertStatus(404, response)
  }

  @Test
  def testPostCelebritySendWelcomeEmailNoAdmin() {
    createAndLoginAsAdmin()
    createCeleb()
    logout()
    val response = POST("/admin/celebrities/1/sendEmail", getPostStrParams("derp@derp.com"))
    assertStatus(302, response)
    //redirect to admin screen
    assertEquals("[/admin/login]", response.headers("Location").toString)
  }

  private def getPostStrParams(celebrityEmail: String): Map[String, String] = {
    Map[String, String]("celebrityEmail" -> celebrityEmail)
  }
}
