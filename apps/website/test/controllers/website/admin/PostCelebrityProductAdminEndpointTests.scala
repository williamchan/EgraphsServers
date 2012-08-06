package controllers.website.admin

import org.junit.Test
import java.io.File
import scala.collection.JavaConversions._
import play.Play
import org.junit.Assert._
import services.AppConfig
import play.test.FunctionalTest._
import models.enums.PublishedStatus
import models.{Product, ProductStore}
import services.db.{Schema, TransactionSerializable, DBSession}
import services.AppConfig._

/**
 * Tests for the PostCelebrityProductAdminEndpoint class for updating and creating celebs
 */

class PostCelebrityProductAdminEndpointTests extends AdminFunctionalTest {
  // Load images for creating products
  private val schema = instance[Schema]
  private val profileImage = Play.getFile("test/files/longoria/product-1.jpg")
  private val profileIcon  = Play.getFile("test/files/longoria/profile.jpg")
  @Test
  def testPostCelebrityProductCreatesProduct() = {
    createAndLoginAsAdmin()
    createCeleb()

    val response = POST("/admin/celebrities/1/products", productParams(), productImages())
    assertStatus(302, response)
    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[ProductStore].get(1).publishedStatus, PublishedStatus.Unpublished)
    }
//    assertHeaderEquals("Location", "/Muhammad-Ali/photos", response)
  }

  @Test
  def testPostCelebrityProductValidatesPublishedStatus() {
    createAndLoginAsAdmin()
    createCeleb()

    val postStrParams: Map[String, String] = productParams(publishedStatusString = "-")
    val postStrImages = productImages()

    val response = POST("/admin/celebrities/1/products", postStrParams, postStrImages)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/celebrities/1/products/create", response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Error setting product's published status, please contact support"))
  }

  @Test
  def testPostCelebrityProductCreatesAndUpdatesStatus() {
    import AppConfig.instance

    createAndLoginAsAdmin()
    createCeleb()

    val response = postCelebrityProductPublishedStatus(status = PublishedStatus.Unpublished.name)

    assertStatus(302, response)
//    assertHeaderEquals("Location", "/Muhammad-Ali/photos", response)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[ProductStore].get(1).publishedStatus, PublishedStatus.Unpublished)
    }

    val publishedResponse = postCelebrityProductPublishedStatus(id = 1, status = PublishedStatus.Published.name)

    assertStatus(302, publishedResponse)
//    assertHeaderEquals("Location", "/admin/products/1?action=preview", publishedResponse)

    instance[DBSession].connected(TransactionSerializable) {
      assertEquals(instance[ProductStore].get(1).publishedStatus, PublishedStatus.Published)
    }

  }

  private def postCelebrityProductPublishedStatus(id: Long = 0, status: String)= {
    POST("/admin/celebrities/1/products", productParams(productId = id, publishedStatusString = status),
      productImages())
  }

  private def productParams(productId: Long = 0,
                            productName: String = "Evan Longoria of the Tampa Bay Rays",
                            productDescription: String =  "Evan Longoria, third baseman for the Rays is a standout player",
                            priceInCurrency: BigDecimal = Product.defaultPrice,
                            signingOriginX: Int = 100,
                            signingOriginY: Int = 100,
                            storyTitle: String = "Walk-off home run for a playoff spot",
                            storyText: String = "Earlier in the month, the Rays were facing a deficit of nine games in " +
                                                "the wild card race to the Boston Red Sox.",
                            publishedStatusString: String =  PublishedStatus.Unpublished.name) : Map[String, String] = {

    Map[String, String](
      "productId" -> productId.toString,
      "productName" -> productName,
      "productDescription" -> productDescription,
      "priceInCurrency" -> priceInCurrency.toString(),
      "signingOriginX" -> signingOriginX.toString,
      "signingOriginY" -> signingOriginY.toString,
      "storyTitle" -> storyTitle,
      "storyText" -> storyText,
      "publishedStatusString" -> publishedStatusString
    )
  }

  private def productImages(productImage: File =  profileImage,
                             productIcon:  File =  profileIcon) : Map[String, File] = {
    Map[String, File](
      "productImage" -> productImage,
      "productIcon" -> productIcon
    )
  }

}
