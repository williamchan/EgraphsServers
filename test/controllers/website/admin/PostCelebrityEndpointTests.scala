package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import java.net.URLDecoder


class PostCelebrityEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  @Test
  def testPostCelebrityCreatesCelebrity() {
    val postParams: Map[String, String] = getPostParams()
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/Muhammad-Ali", response)
  }

  @Test
  def testPostCelebrityCreatesCelebrityWithFullNameAsPublicName() {
    val postParams: Map[String, String] = getPostParams(publicName = "")
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/Cassius-Clay", response)
  }

  @Test
  def testPostCelebrityValidatesFields() {
    val postParams: Map[String, String] = getPostParams("", "", "", "", "", "")
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:E-mail address,Password,Description"))
  }

  @Test
  def testPostCelebrityValidatesEmail() {
    val postParams: Map[String, String] = getPostParams(
      celebrityEmail = "not a valid email"
    )
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("E-mail address"))
  }

  @Test
  def testPostCelebrityRequiresEitherFullNameOrPublicName() {
    val errorString = "Must provide either Public Name or First and Last Name"

    val withFirstName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostParams(firstName = "Cassius", lastName = "", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(withFirstName.contains(errorString))

    val withLastName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostParams(firstName = "", lastName = "Clay", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(withLastName.contains(errorString))

    val withFullName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostParams(firstName = "Cassius", lastName = "Clay", publicName = "")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(!withFullName.contains(errorString))

    val withPublicName: String = URLDecoder.decode(POST("/admin/celebrities",
      getPostParams(firstName = "", lastName = "", publicName = "Muhammad Ali")).cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(!withPublicName.contains(errorString))
  }

  @Test
  def testPostCelebrityValidatesThatNoCelebrityWithSameEmailExists() {
    assertHeaderEquals("Location", "/Muhammad-Ali", POST("/admin/celebrities", getPostParams()))

    val postParams: Map[String, String] = getPostParams(
      publicName = "Cassius Clay"
    )
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Celebrity with e-mail address already exists"))
  }

//  @Test
//  def testPostCelebrityChecksThatPasswordMatchesExistingPasswordOnAccount() {
//    Account(email="wchan83@egraphs.com").withPassword("derp").right.get.save()
//
//    val postParams: Map[String, String] = getPostParams(
//      celebrityEmail = "wchan83@egraphs.com",
//      celebrityPassword = "-"
//    )
//    val response = POST("/admin/celebrities", postParams)
//
//    assertStatus(302, response)
//    assertHeaderEquals("Location", "/admin/celebrities/create", response)
//    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
//    assertTrue(decodedCookieValue.contains("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account"))
//  }

  @Test
  def testPostCelebrityValidatesPassword() {
    val postParams: Map[String, String] = getPostParams(
      celebrityPassword = "-"
    )
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:password"))
  }

  @Test
  def testPostCelebrityValidatesCelebrityUrlSlugIsUnique() {
    assertHeaderEquals("Location", "/Muhammad-Ali", POST("/admin/celebrities", getPostParams()))

    val postParams: Map[String, String] = getPostParams(
      celebrityEmail = "ali2@egraphs.com"
    )
    val response = POST("/admin/celebrities", postParams)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/create", response)
    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    assertTrue(decodedCookieValue.contains("errors:Celebrity with same website name exists. Provide different public name"))
  }

  private def getPostParams(celebrityEmail: String = "ali@egraphs.com",
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
