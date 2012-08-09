package controllers.website


import admin.AdminFunctionalTest
import org.junit.Test
import play.test.FunctionalTest
import FunctionalTest._
import services.db.TransactionSerializable
import utils.TestData
import controllers.WebsiteControllers
import services.AppConfig
import models.AccountStore


class GetVerifyAccountEndpointTests extends AdminFunctionalTest {
  private val accountStore = AppConfig.instance[AccountStore]

  val url = WebsiteControllers.reverse(WebsiteControllers.getVerifyAccount()).url
  @Test
  def testVerifyingWithIncorrectSecurityFails() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val response = GET(url + "?email=" + account.email + "&secretKey=derpderp")
    assertStatus(403, response)
  }

  @Test
  def testVerifyingWithIncorrectEmailFails() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = GET(url + "?email=" + "derper@derp.org" + "&secretKey=" + account.email)
    assertStatus(404, response)
  }

  @Test
  def testVerifyingWithCorrectCredentialsActuallyVerifies() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }


    val response = GET(url + "?email=" + account.email + "&secretKey=" + account.resetPasswordKey.getOrElse("error"))
    assertStatus(200, response)

    val accountUpdated = db.connected(TransactionSerializable) {
       accountStore.findByEmail(account.email)
    }

    assert(accountUpdated.get.emailVerified)


  }
}
