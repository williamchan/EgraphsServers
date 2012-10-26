package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.db.TransactionSerializable
import utils.TestData
import controllers.WebsiteControllers

class PostRecoverAccountEndpointTests extends EgraphsFunctionalTest {
  val url = WebsiteControllers.reverse(WebsiteControllers.postRecoverAccount()).url
  @Test
  def testEmailValidation() {
    val response = POST(url, getPostStrParams(email = ""))
    assertStatus(404, response)
  }

  @Test
  def testRecoverAccountSetsResetPasswordKey() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val response = POST(url, getPostStrParams(email = account.email))
    assertFalse(getPlayFlashCookie(response).contains("error"))
  }

  private def getPostStrParams(email: String): Map[String, String] = {
    Map[String, String]("email" -> email)
  }
}
