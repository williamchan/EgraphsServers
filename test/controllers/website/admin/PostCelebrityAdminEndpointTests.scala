package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import java.net.URLDecoder
import services.AppConfig
import models.{PublishedStatus, CelebrityStore}
import services.db.{TransactionSerializable, DBSession}
import play.mvc.Http.Response

class PostCelebrityAdminEndpointTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  @Test
  def testPostCelebrityCreatesCelebrity() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams()
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
  }

  @Test
  def testPostCelebrityCreatesCelebrityWithFullNameAsPublicName() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(publicName = "")
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
  }

  @Test
  def testPostCelebrityValidatesFields() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(0, "", "", "", "", "", "")
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Description,Password,E-mail address"))
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
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("E-mail address"))
  }

  @Test
  def testPostCelebrityRequiresEitherFullNameOrPublicName() {
    createAndLoginAsAdmin()

    val errorString = "Must provide either Public Name or First and Last Name"

    val withFirstName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostCelebrityStrParams(firstName = "Cassius", lastName = "", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(withFirstName.contains(errorString))

    val withLastName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostCelebrityStrParams(firstName = "", lastName = "Clay", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(withLastName.contains(errorString))

    val withFullName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostCelebrityStrParams(firstName = "Cassius", lastName = "Clay", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(!withFullName.contains(errorString))

    val withPublicName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostCelebrityStrParams(firstName = "", lastName = "", publicName = "Muhammad Ali")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(!withPublicName.contains(errorString))
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
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Celebrity with e-mail address already exists"))
  }

//  @Test
//  def testPostCelebrityChecksThatPasswordMatchesExistingPasswordOnAccount() {
//    Account(email="wchan83@egraphs.com").withPassword("derp").right.get.save()
//
//    val postStrParams: Map[String, String] = getPostParams(
//      celebrityEmail = "wchan83@egraphs.com",
//      celebrityPassword = "-"
//    )
//    val response = POST("/admin/celebrities", postStrParams)
//
//    assertStatus(302, response)
//    assertHeaderEquals("Location", "/admin/celebrities/create", response)
//    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
//    assertTrue(decodedCookieValue.contains("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account"))
//  }

  @Test
  def testPostCelebrityValidatesPassword() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(
      celebrityPassword = "-"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:password"))
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
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Celebrity with same website name exists. Provide different public name"))
  }

  @Test
  def testPostCelebrityValidatesPublishedStatus() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostCelebrityStrParams(publishedStatusString = "-")

    val response = POST("/admin/celebrities", postStrParams)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Error setting celebrity's published status, please contact support"))
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



  private def postCelebrityPublishedStatus(id: Long = 0, status: String) : Response = {
    val postStrParams = getPostCelebrityStrParams(publishedStatusString = status, celebrityId = id)
    POST("/admin/celebrities", postStrParams)
  }
}
