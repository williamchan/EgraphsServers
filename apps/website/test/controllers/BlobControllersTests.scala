package controllers

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import play.Play
import website.EgraphsFunctionalTest
import utils.TestHelpers

class BlobControllersTests extends EgraphsFunctionalTest {

  import FunctionalTest._

  @Test
  def testBlobIsAccessibleViaEgraphsLink() {
    TestHelpers.putPublicImageOnBlobStore()

    val response = GET("/blob/files/a/b/derp.jpg")
    assertIsOk(response)

    val actualFile = Play.getFile("./test/files/derp.jpg")

    assertEquals(actualFile.length, response.out.toByteArray.length)
  }

}