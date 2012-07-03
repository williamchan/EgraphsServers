package controllers.website

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.TestData
import controllers.website.GetVerifyAccountEndpoint

class GetVerifyAccountEndpointTests extends EgraphsFunctionalTest {
  private val db = AppConfig.instance[DBSession]

  @Test
  def testVerification() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = GET(
      "/account/verify?accountId=" + account.customerId.get +
      "&key=" + account.resetPasswordKey.get)

    assertStatus(200, response)
  }

  def testIgnoreIncorrectKeys() {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }

    val response = GET(
      "/account/verify?accountId=" + account.customerId.get +
        "&key=" + "herpaderp")
    //forbidden
    assertStatus(403, response)
  }

}


