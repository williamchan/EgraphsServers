package controllers

import java.io.File
import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, runScenario}


class DBTransactionTests extends FunctionalTest with CleanDatabaseAfterEachTest {

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
    runScenario("A-public-image-is-on-the-blobstore")

    val response = GET("/test/files/a/b/derp.jpg")
    assertIsOk(response)

    val actualFile = new File("./test/files/derp.jpg")

    assertEquals(actualFile.length, response.out.toByteArray.length)
  }

}