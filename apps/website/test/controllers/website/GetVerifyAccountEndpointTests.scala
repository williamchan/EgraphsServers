package controllers.website


import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import services.db.TransactionSerializable
import utils.TestData
import controllers.routes.WebsiteControllers.getVerifyAccount
import services.AppConfig
import models.AccountStore
import models.Account
import utils.EgraphsUnitTest
import services.db.DBSession


class GetVerifyAccountEndpointTests extends EgraphsUnitTest {
  private def accountStore = AppConfig.instance[AccountStore]
  private def db = AppConfig.instance[DBSession]
  
  routeName(getVerifyAccount("customer-email", "reset-password-key")) should "forbid verifying with incorrect reset key" in new EgraphsTestApplication {
    val account = makeAccountWithResetPasswordKey
    
    status(performRequest(account)(secretKey="herpderp")) should be (FORBIDDEN)
  }

  it should "not find the reset password endpoint for an email that doesn't correspond to an account" in new EgraphsTestApplication {
    val account = makeAccountWithResetPasswordKey

    status(performRequest(account)(email="derper@derp.org")) should be (NOT_FOUND)
  }

  it should "service requests with a valid email and corresponding 'reset password' key" in new EgraphsTestApplication {
    val account = makeAccountWithResetPasswordKey
    
    status(performRequest(account)()) should be (OK)

    val accountUpdated = db.connected(TransactionSerializable) {
       accountStore.findByEmail(account.email)
    }

    assert(accountUpdated.get.emailVerified)
  }
  
  private def makeAccountWithResetPasswordKey(): Account = {
    db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account.withResetPasswordKey.save()
    }
  }
  
  private def performRequest(
    account: Account
  )(
    email: String=account.email,
    secretKey: String=account.resetPasswordKey.getOrElse(throw new RuntimeException("Expected reset password key"))
  ): play.api.mvc.Result = 
  { 
    controllers.WebsiteControllers.getVerifyAccount(email, secretKey)(FakeRequest())
  }
}
