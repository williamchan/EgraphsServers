package controllers.website.admin

import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.runScenarios
import scala.collection.JavaConversions._
import play.mvc.Http.Response
import controllers.website.EgraphsFunctionalTest
import models.enums.PublishedStatus
import utils.TestData

trait AdminFunctionalTest extends EgraphsFunctionalTest {

  def createAdmin() {
    runScenarios("Create-Admin")
  }

  def createAndLoginAsAdmin(): Response = {
    createAdmin()
    loginAsAdmin()
  }

  def loginAsAdmin(): Response = {
    val response = POST("/admin/login", Map[String, String]("email" -> "admin@egraphs.com", "password" -> TestData.defaultPassword))
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities", response)
    response
  }

  def createCeleb(): Response = {
    val response = POST("/admin/celebrities", getPostCelebrityStrParams())
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
    response
  }

  def getPostCelebrityStrParams(celebrityId: Long = 0,
                                        celebrityEmail: String = "ali@egraphs.com",
                                        celebrityPassword: String = TestData.defaultPassword,
                                        firstName: String = "Cassius",
                                        lastName: String = "Clay",
                                        publicName: String = "Muhammad Ali",
                                        description: String = "I am the greatest!",
                                        publishedStatusString: String = PublishedStatus.Published.name ): Map[String, String] = {
    Map[String, String](
      "celebrityId" -> celebrityId.toString,
      "celebrityEmail" -> celebrityEmail,
      "celebrityPassword" -> celebrityPassword,
      "firstName" -> firstName,
      "lastName" -> lastName,
      "publicName" -> publicName,
      "description" -> description,
      "publishedStatusString" -> publishedStatusString
    )
  }
}
