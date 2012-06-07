package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.TestData

class PostResetPasswordEndpointTests extends EgraphsFunctionalTest {

  private val db = AppConfig.instance[DBSession]

  @Test
  def testFailPasswordsMustMatchValidation() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = POST("/account/reset", getPostStrParams(email = account.email, password = "password1", password2 = "password2"))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("Passwords do not match"))
  }

  @Test
  def testFailPasswordMustPassStrengthTestValidation() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = POST("/account/reset", getPostStrParams(email = account.email, password = "p", password2 = "p"))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors"))
  }

  @Test
  def testSetsNewPassword() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val response = POST("/account/reset", getPostStrParams(email = account.email, password = "password", password2 = "password"))
    assertFalse(getPlayFlashCookie(response).contains("error"))
    // Unfortunately, there is no way to check the session
  }

  private def getPostStrParams(email: String, password: String, password2: String): Map[String, String] = {
    Map[String, String]("email" -> email, "password" -> password, "password2" -> password2)
  }
}
