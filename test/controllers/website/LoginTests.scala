package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.TestData

class LoginTests extends EgraphsFunctionalTest {

  private val db = AppConfig.instance[DBSession]

  @Test
  def testEmailAndPasswordValidation() {
    val response = POST("/login", getPostStrParams(email = "", password = ""))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors:Email,Password"))
  }

  @Test
  def testAuthenticationValidation() {
    val response = POST("/login", getPostStrParams(email = "idontexist@egraphs.com", password = TestData.defaultPassword))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("The username or password did not match. Please try again."))
  }

  @Test
  def testSuccessfulLogin() {
    val password = "password"
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withPassword(password).right.get.save()
    }

    val response = login(account, password)
    assertFalse(getPlayFlashCookie(response).contains("error"))
    // Unfortunately, there is no way to check the session
  }

  private def getPostStrParams(email: String, password: String): Map[String, String] = {
    Map[String, String]("email" -> email, "password" -> password)
  }
}
