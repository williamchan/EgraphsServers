package controllers.website

import models.AccountStore
import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import utils.FunctionalTestUtils._
import services.db.TransactionSerializable
import utils.TestData
import controllers.routes.WebsiteControllers.postRecoverAccount
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.CsrfProtectedResourceTests

class PostRecoverAccountEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests {

  private def accountStore = AppConfig.instance[AccountStore]
  private def db = AppConfig.instance[DBSession]
  
  override protected def routeUnderTest = postRecoverAccount 

  // If given an invalid email address, it should redirect back to the get controller,
  // showing the error 'Account not found for given email'.
  routeName(postRecoverAccount()) should "show errors if given an invalid email address" in new EgraphsTestApplication {

    val Some(result) = route(
      FakeRequest().toCall(postRecoverAccount).withFormUrlEncodedBody(
        "email" -> "gnuggets@gangstaville.com").withAuthToken
    )

    redirectLocation(result) should be (Some(
      controllers.routes.WebsiteControllers.getRecoverAccount.url))
  }

  it should "set the account's 'reset password' key" in new EgraphsTestApplication {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val Some(result) = route(
      FakeRequest()
        .toCall(postRecoverAccount)
        .withFormUrlEncodedBody("email" -> account.email)
        .withAuthToken
    )

    // We expect a redirect to a success message here.
    redirectLocation(result) should be (Some(controllers.routes.WebsiteControllers.getSimpleMessage(
      header = "Success",
      body = "Instructions for recovering your account have been sent to your email address.").url))

    val updatedAccount = db.connected(TransactionSerializable) {
      accountStore.findByEmail(account.email).get
    }

    // We expect the resetPasswordKey to have been defined in the post controller
    updatedAccount.resetPasswordKey should be ('defined)
  }
}
