package controllers.website

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import services.db.TransactionSerializable
import utils.TestData
import controllers.routes.WebsiteControllers.postRecoverAccount
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.CsrfProtectedResourceTests

class PostRecoverAccountEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests {
  
  private def db = AppConfig.instance[DBSession]
  
  override protected def routeUnderTest = postRecoverAccount 

  routeName(postRecoverAccount()) should "validate email addresses" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(
      FakeRequest().toRoute(postRecoverAccount).withFormUrlEncodedBody("email" -> "gnuggets@gangstaville.com").withAuthToken
    )
    
    status(result) should be (NOT_FOUND)
  }

  it should "set the account's 'reset password' key" in new EgraphsTestApplication {
    val account = db.connected(TransactionSerializable) {
      val customer = TestData.newSavedCustomer()
      customer.account
    }

    val Some(result) = routeAndCall(
      FakeRequest()
        .toRoute(postRecoverAccount)
        .withFormUrlEncodedBody("email" -> account.email)
        .withAuthToken
    )
    
    val Some(flash) = result.flash
    
    status(result) should be (OK)
    flash.get("errors") should be (None)
  }
}
