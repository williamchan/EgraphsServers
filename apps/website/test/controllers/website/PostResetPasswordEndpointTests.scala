package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.{Utils, AppConfig}
import services.db.{TransactionSerializable, DBSession}
import utils.TestData

class PostResetPasswordEndpointTests extends EgraphsFunctionalTest {

  private val db = AppConfig.instance[DBSession]
  private val url = Utils.lookupUrl("WebsiteControllers.postResetPassword").url

  @Test
  def testFailPasswordsMustMatchValidation() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }
    val response = POST(url, getPostStrParams(secretKey = account.resetPasswordKey.get, email = account.email,
      newPassword = "password1", passwordConfirm = "password2"))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors"))
  }

  @Test
  def testFailPasswordMustPassStrengthTestValidation() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }


    val response = POST(url, getPostStrParams(secretKey = account.resetPasswordKey.get,email = account.email,
      newPassword = "p", passwordConfirm = "p"))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors"))
  }

  @Test
  def testSetsNewPassword() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = POST(url, getPostStrParams(secretKey = account.resetPasswordKey.get, email = account.email,
      newPassword = "password", passwordConfirm = "password"))
    println("response: " + response)
    assertStatus(200, response)
    assertFalse(getPlayFlashCookie(response).contains("error"))
    // Unfortunately, there is no way to check the session
  }

  private def getPostStrParams(secretKey: String, email: String, newPassword: String, passwordConfirm: String): Map[String, String] = {
    Map[String, String]("secretKey" -> secretKey, "email" -> email, "newPassword" -> newPassword, "passwordConfirm" -> passwordConfirm)
  }
}
