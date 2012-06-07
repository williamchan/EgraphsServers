package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.TestData

class PostRecoverAccountEndpointTests extends EgraphsFunctionalTest {

  private val db = AppConfig.instance[DBSession]

  @Test
  def testEmailValidation() {
    val response = POST("/account/recover", getPostStrParams(email = ""))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Email"))
  }

  @Test
  def testRecoverAccountSetsResetPasswordKey() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val response = POST("/account/recover", getPostStrParams(email = account.email))
    assertFalse(getPlayFlashCookie(response).contains("error"))
  }

  private def getPostStrParams(email: String): Map[String, String] = {
    Map[String, String]("email" -> email)
  }
}
