package controllers.website.admin

import org.junit.Test
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import java.io.File
import scala.collection.JavaConversions._
import play.Play
import java.net.URLDecoder
import org.junit.Assert._
import services.AppConfig
import services.AppConfig._
import services.db.{TransactionSerializable, DBSession}
import play.test.FunctionalTest._
import models.{ProductStore, CelebrityStore, PublishedStatus}

class PostCelebrityProductAdminEndpointTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  private val profileImage = Play.getFile("test/files/longoria/product-1.jpg")
  private val profileIcon  = Play.getFile("test/files/longoria/profile.jpg")
  @Test
  def testPostCelebrityProductCreatesPublishedProduct() = {
    createAndLoginAsAdmin()
    createCeleb()

    val response = POST("/admin/celebrities/1/products", getCelebParams(), getCelebImages())
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/products/1?action=preview", response)
  }

  @Test
  def testPostCelebrityProductValidatesPublishedStatus() {
    createAndLoginAsAdmin()
    createCeleb()

    val postStrParams: Map[String, String] = getCelebParams(publishedStatusString = "-")
    val postStrImages = getCelebImages()

    val response = POST("/admin/celebrities/1/products", postStrParams, postStrImages)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1/products/create", response)

    val decodedCookieValue: String = URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
    println("Decoded Cookie:")
    assertTrue(decodedCookieValue.contains("errors:Error setting product's published status, please contact support"))
  }

  @Test
  def testPostCelebrityProductCreatesAndUpdatesStatus() {
    import AppConfig.instance

    createAndLoginAsAdmin()
    createCeleb()

    val response = postCelebrityProductPublishedStatus(status = PublishedStatus.Unpublished.name)

    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/products/1?action=preview", response)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[ProductStore].get(1).publishedStatus, PublishedStatus.Unpublished)
    }

    val publishedResponse = postCelebrityProductPublishedStatus(id = 1, status = PublishedStatus.Published.name)

    assertStatus(302, publishedResponse)
    assertHeaderEquals("Location", "/admin/products/1?action=preview", response)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[ProductStore].get(1).publishedStatus, PublishedStatus.Published)
    }

  }

  private def postCelebrityProductPublishedStatus(id: Long = 0, status: String)= {
    POST("/admin/celebrities/1/products", getCelebParams(productId = id, publishedStatusString = status),
      getCelebImages())
  }

  private def getCelebParams(productId: Long = 0,
                              productName: String = "Evan Longoria of the Tampa Bay Rays",
                              productDescription: String =  "Evan Longoria, third baseman for the Rays is a standout player",
                              signingOriginX: Int = 100,
                              signingOriginY: Int = 100,
                              storyTitle: String = "Walk-off home run for a playoff spot",
                              storyText: String = "Earlier in the month, the Rays were facing a deficit of nine games in " +
                                                  "the wild card race to the Boston Red Sox.",
                              publishedStatusString: String =  PublishedStatus.Published.name) : Map[String, String] = {

    Map[String, String](
      "productID" -> productId.toString,
      "productName" -> productName,
      "productDescription" -> productDescription,
      "signingOriginX" -> signingOriginX.toString,
      "signingOriginY" -> signingOriginY.toString,
      "storyTitle" -> storyTitle,
      "storyText" -> storyText,
      "publishedStatusString" -> publishedStatusString
    )
  }

  private def getCelebImages(productImage: File =  profileImage,
                             productIcon:  File =  profileIcon) : Map[String, File] = {
    Map[String, File](
      "productImage" -> productImage,
      "productIcon" -> productIcon
    )
  }

}
