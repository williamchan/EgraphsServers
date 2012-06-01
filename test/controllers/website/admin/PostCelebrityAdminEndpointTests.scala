package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._

class PostCelebrityAdminEndpointTests extends AdminFunctionalTest {

  @Test
  def testPostCelebrityCreatesCelebrity() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostStrParams()
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
  }

  @Test
  def testPostCelebrityCreatesCelebrityWithFullNameAsPublicName() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostStrParams(publicName = "")
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", response)
  }

  @Test
  def testPostCelebrityValidatesFields() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostStrParams("", "", "", "", "", "")
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Description,Password,E-mail address"))
  }

  @Test
  def testPostCelebrityValidatesEmail() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostStrParams(
      celebrityEmail = "not a valid email"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("E-mail address"))
  }

  @Test
  def testPostCelebrityRequiresEitherFullNameOrPublicName() {
    createAndLoginAsAdmin()

    val errorString = "Must provide either Public Name or First and Last Name"

    val responseWithFirstName = POST("/admin/celebrities", getPostStrParams(firstName = "Cassius", lastName = "", publicName = ""))
    assertTrue(getPlayFlashCookie(responseWithFirstName).contains(errorString))

    val responseWithLastName = POST("/admin/celebrities", getPostStrParams(firstName = "", lastName = "Clay", publicName = ""))
    assertTrue(getPlayFlashCookie(responseWithLastName).contains(errorString))

    val responseWithFullName = POST("/admin/celebrities", getPostStrParams(firstName = "Cassius", lastName = "Clay", publicName = ""))
    assertTrue(!getPlayFlashCookie(responseWithFullName).contains(errorString))

    val responseWithPublicName = POST("/admin/celebrities", getPostStrParams(firstName = "", lastName = "", publicName = "Muhammad Ali"))
    assertTrue(!getPlayFlashCookie(responseWithPublicName).contains(errorString))
  }

  @Test
  def testPostCelebrityValidatesThatNoCelebrityWithSameEmailExists() {
    createAndLoginAsAdmin()

    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", POST("/admin/celebrities", getPostStrParams()))

    val postStrParams: Map[String, String] = getPostStrParams(
      publicName = "Cassius Clay"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Celebrity with e-mail address already exists"))
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
//    assertTrue(getPlayFlashCookie(response).contains("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account"))
//  }

  @Test
  def testPostCelebrityValidatesPassword() {
    createAndLoginAsAdmin()

    val postStrParams: Map[String, String] = getPostStrParams(
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

    assertHeaderEquals("Location", "/admin/celebrities/1?action=preview", POST("/admin/celebrities", getPostStrParams()))

    val postStrParams: Map[String, String] = getPostStrParams(
      celebrityEmail = "ali2@egraphs.com"
    )
    val response = POST("/admin/celebrities", postStrParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Celebrity with same website name exists. Provide different public name"))
  }

  private def getPostStrParams(celebrityEmail: String = "ali@egraphs.com",
                               celebrityPassword: String = "derp",
                               firstName: String = "Cassius",
                               lastName: String = "Clay",
                               publicName: String = "Muhammad Ali",
                               description: String = "I am the greatest!"): Map[String, String] = {
    Map[String, String](
      "celebrityEmail" -> celebrityEmail,
      "celebrityPassword" -> celebrityPassword,
      "firstName" -> firstName,
      "lastName" -> lastName,
      "publicName" -> publicName,
      "description" -> description
    )
  }
}
