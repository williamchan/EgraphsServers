package controllers

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import play.Play
import website.EgraphsFunctionalTest
import utils.TestHelpers

class DBTransactionTests extends EgraphsFunctionalTest {

  import FunctionalTest._

  @Test
  def testSucceedingRequestCommitsTransaction() {
    assertIsOk(GET("/test/request-transaction/without-error"))
    assertEquals("Yep", getContent(GET("/test/request-transaction/is-stored")))
  }

  @Test
  def testFailingTransactionDoesntCommit() {
    try {
      GET("/test/request-transaction/with-error")
    } catch {
      case _ => 1 // This failure was expected
    }

    assertEquals("Nope", getContent(GET("/test/request-transaction/is-stored")))
  }

  @Test
  def testBlobIsAccessibleViaEgraphsLink() {
    TestHelpers.putPublicImageOnBlobStore()

    val response = GET("/blob/files/a/b/derp.jpg")
    assertIsOk(response)

    val actualFile = Play.getFile("./test/files/derp.jpg")

    assertEquals(actualFile.length, response.out.toByteArray.length)
  }

}