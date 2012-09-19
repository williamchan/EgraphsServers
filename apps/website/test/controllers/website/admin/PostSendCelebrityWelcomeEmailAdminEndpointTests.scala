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
    val response = POST("/admin/celebrities/1/sendEmail")
    assertStatus(302, response)
  }

  @Test
  def testPostCelebritySendWelcomeEmailNoCeleb() {
    createAndLoginAsAdmin()
    val response = POST("/admin/celebrities/1/sendEmail")
    assertStatus(404, response)
  }

  @Test
  def testPostCelebritySendWelcomeEmailNoAdmin() {
    createCeleb()
    val response = POST("/admin/celebrities/1/sendEmail")
    assertStatus(404, response)
  }

}
