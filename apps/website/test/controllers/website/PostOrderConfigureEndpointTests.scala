package controllers.website

import play.api.test._
import play.api.test.Helpers._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.{postOrderPrivacy, getLogin}
import scala.collection.JavaConversions._
import services.AppConfig
import models.OrderStore
import services.db.TransactionSerializable
import utils.TestData
import models.enums.PrivacyStatus
import utils.EgraphsUnitTest
import models.Customer
import models.Order
import services.db.DBSession
import play.api.mvc.Result
import play.api.mvc.AnyContent
import utils.CsrfProtectedResourceTests

class PostOrderConfigureEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests {
  private def orderStore = AppConfig.instance[OrderStore]
  private def db = AppConfig.instance[DBSession]
  
  override protected def routeUnderTest = postOrderPrivacy(1L)

  routeName(postOrderPrivacy(1L)) should "change privacy status of order when requested by owner and no one else" in new EgraphsTestApplication {
    val (order, recipient, stranger) = db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder().withPrivacyStatus(PrivacyStatus.Private).save()

      (order, order.recipient, TestData.newSavedCustomer())
    }

    private def perform[A](
      request: FakeRequest[A], 
      newPrivacy: String=PrivacyStatus.Public.name
    ): Result = 
    {
      controllers.WebsiteControllers.postOrderPrivacy(order.id).apply(
        request.withFormUrlEncodedBody("privacyStatus" -> newPrivacy).withAuthToken
      )
    }

    // Try to change as a visitor or a stranger. This should fail.
    val visitorResult = perform(FakeRequest())
    val strangerResult = perform(FakeRequest().withCustomer(stranger.id))

    status(strangerResult) should be (FORBIDDEN)
    status(visitorResult) should be (SEE_OTHER)
    redirectLocation(visitorResult) should be (Some(getLogin.url))
    orderPrivacy(order.id) should be (PrivacyStatus.Private)

    // Try to change as the owner. This should succeed
    val ownerResult = perform(FakeRequest().withCustomer(recipient.id))
    
    status(ownerResult) should be (OK)
    orderPrivacy(order.id) should be (PrivacyStatus.Public)
    
    // Try to set as invalid privacy status. This should fail
    val invalidStringResult = perform(FakeRequest().withCustomer(recipient.id), "badprivacy")
  }

  private def orderPrivacy(orderId: Long): PrivacyStatus.EnumVal = {
    db.connected(TransactionSerializable) {
      orderStore.get(orderId).privacyStatus
    }
  }
}
