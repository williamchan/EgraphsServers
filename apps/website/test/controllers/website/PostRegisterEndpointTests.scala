package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.TestData
import models.{AccountStore, CustomerStore}

class PostRegisterEndpointTests extends EgraphsFunctionalTest {

  private val db = AppConfig.instance[DBSession]
  private val accountStore = AppConfig.instance[AccountStore]
  private val customerStore = AppConfig.instance[CustomerStore]

  @Test
  def testFieldValidations() {
    var response = POST("/register", getPostStrParams(email = "a@egraphs.com", name = ""))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors"))

    response = POST("/register", getPostStrParams(email = ""))
    assertTrue(getPlayFlashCookie(response).contains("errors"))

    response = POST("/register", getPostStrParams(email = "a@egraphs.com", password = "herpherp", password2 = "derpderp"))
    assertTrue(getPlayFlashCookie(response).contains("Passwords do not match"))
  }

  @Test
  def testFailPasswordStrengthValidations() {
    val response = POST("/register", getPostStrParams(email = "a@egraphs.com", password = "h", password2 = "h"))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("errors"))
  }

  @Test
  def testFailCustomerAlreadyExistsValidation() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val response = POST("/register", getPostStrParams(email = account.email))
    assertStatus(302, response)
    assertTrue(getPlayFlashCookie(response).contains("Account already exists"))
  }

  @Test
  def testCreatesCustomerIfAccountExistsButLacksCustomer() {
    var account = db.connected(TransactionSerializable) {
      TestData.newSavedAccount()
    }

    val customerName = "asdf"
    val response = POST("/register", getPostStrParams(email = account.email, name = customerName))
    assertStatus(302, response)
    db.connected(TransactionSerializable) {
      account = accountStore.findById(account.id).get
      assertEquals(customerName, customerStore.findById(account.customerId.get).get.name)
    }
  }

  @Test
  def testCreatesCustomerIfAccountDoesNotExist() {
    val email = "a@egraphs.com"
    val response = POST("/register", getPostStrParams(email = email))
    assertStatus(302, response)
    db.connected(TransactionSerializable) {
      val account = accountStore.findByEmail(email).get
      assertEquals(account.customerId.get, customerStore.findById(account.customerId.get).get.id)
    }
  }

  private def getPostStrParams(email: String,
                               password: String = TestData.defaultPassword,
                               password2: String = TestData.defaultPassword,
                               name: String = "Joe Customer"): Map[String, String] = {
    Map[String, String]("email" -> email, "password" -> password, "password2" -> password2, "name" -> name)
  }
}
