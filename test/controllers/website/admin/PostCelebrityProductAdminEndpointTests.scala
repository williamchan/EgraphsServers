package controllers.website.admin

import org.junit.Test
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import models.PublishedStatus
import java.io.File
import scala.collection.JavaConversions._
import play.Play
import play.test.FunctionalTest._

class PostCelebrityProductAdminEndpointTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  @Test
  def testPostCelebrityProductCreatesProduct() = {
    createAndLoginAsAdmin()
    createCeleb()

    val response = POST("/admin/celebrities/1/products", getCelebParams(), getCelebImages())
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/products/1?action=preview", response)

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

  private def getCelebImages(productImage: File =  Play.getFile("test/files/longoria/product-1.jpg"),
                             productIcon:  File =  Play.getFile("test/files/longoria/profile.jpg")) : Map[String, File] = {
    Map[String, File](
      "productImage" -> productImage,
      "productIcon" -> productIcon
    )
  }

}
