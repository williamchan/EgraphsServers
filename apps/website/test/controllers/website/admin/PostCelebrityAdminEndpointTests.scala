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

class PostCelebrityAdminEndpointTests extends AdminFunctionalTest {

  @Test
  def testPostCelebrityCreatesCelebrity() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams()
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
  }

  @Test
  def testPostCelebrityValidatesEmail() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(
      celebrityEmail = "not a valid email"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("E-mail address"))
  }

  @Test
  def testPostCelebrityValidatesThatNoCelebrityWithSameEmailExists() {
    createAndLoginAsAdmin()

    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", POST("/admin/celebrities", getPostCelebrityStrParams()))

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(
      publicName = "Cassius Clay"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Celebrity with e-mail address already exists"))
  }

  @Test
  def testPostCelebrityValidatesPassword() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(
      celebrityPassword = "-"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:password"))
  }

  @Test
  def testPostCelebrityValidatesCelebrityUrlSlugIsUnique() {
    createAndLoginAsAdmin()

    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", POST("/admin/celebrities", getPostCelebrityStrParams()))

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(
      celebrityEmail = "ali2@egraphs.com"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Celebrity with same website name exists. Provide different public name"))
  }

  @Test
  def testPostCelebrityValidatesPublishedStatus() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(publishedStatusString = "-")

    val response = POST("/admin/celebrities", postStrParams)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Error setting celebrity's published status, please contact support"))
  }

  @Test
  def testPostCelebrityCreatesAndUpdatesStatus() {
    import AppConfig.instance

    createAndLoginAsAdmin()
    val response = postCelebrityPublishedStatus(status = PublishedStatus.Unpublished.name)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[CelebrityStore].get(1).publishedStatus, PublishedStatus.Unpublished)
    }

    val publishedResponse = postCelebrityPublishedStatus(id = 1, status = PublishedStatus.Published.name)

    assertStatus(302, publishedResponse)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[CelebrityStore].get(1).publishedStatus, PublishedStatus.Published)
    }

  }
  // Helper function for toggling publishedStatus
  private def postCelebrityPublishedStatus(id: Long = 0, status: String) : Response = {
    val postStrParams = getPostCelebrityStrParams(publishedStatusString = status, celebrityId = id)
    POST("/admin/celebrities", postStrParams)
  }
}
