package controllers.website

import play.api.test.Helpers._
import play.api.test._
import play.api.mvc.Result
import services.db.TransactionSerializable
import utils.{TestConstants, TestData}
import models.enums.{EgraphState, OrderReviewStatus, PrivacyStatus}
import services.AppConfig
import services.db.DBSession
import utils.EgraphsUnitTest
import egraphs.playutils.Encodings.Base64
import controllers.routes.WebsiteControllers.getEgraph
import utils.FunctionalTestUtils
import FunctionalTestUtils.Conversions._
import utils.FunctionalTestUtils._

class GetEgraphEndpointTests extends EgraphsUnitTest {
  
  private def db = AppConfig.instance[DBSession]
  
  "A private egraph" should "only be viewable by buyer, recipient, and admin" in new EgraphsTestApplication {
    val (orderId, buyer, recipient, anotherCustomer, admin) = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val recipient = TestData.newSavedCustomer()
      val anotherCustomer = TestData.newSavedCustomer()
      val admin = TestData.newSavedAdministrator()
      val order = buyer.buyUnsafe(TestData.newSavedProduct(), recipient = recipient)
        .withPrivacyStatus(PrivacyStatus.Private)
        .withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      order.newEgraph.withEgraphState(EgraphState.Published)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Base64.decode(TestConstants.voiceStr())).save()
      (order.id, buyer, recipient, anotherCustomer, admin)
    }
	
    val requestAsCustomer: Option[Long] => Result = egraphRequestAsCustomer(orderId, _)
    
    // anonymous users and random customers are redirected away
    status(requestAsCustomer(None)) should be (FORBIDDEN)
    status(requestAsCustomer(Some(anotherCustomer.id))) should be (FORBIDDEN)
    
    // buyer, recipient, and admins are able to view this egraph
    status(requestAsCustomer(Some(buyer.id))) should be (OK)
    status(requestAsCustomer(Some(recipient.id))) should be (OK)
    
    val adminReq = FakeRequest(GET, getEgraph(orderId).url).withAdmin(admin.id)
    status(route(adminReq).get) should be (OK)
  }
  
  "A public egraph" should "be viewable by all" in new EgraphsTestApplication {
    val orderId: Long = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val order = buyer.buyUnsafe(TestData.newSavedProduct())
        .withReviewStatus(OrderReviewStatus.ApprovedByAdmin).save()
      order.newEgraph.withEgraphState(EgraphState.Published)
        .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), Base64.decode(TestConstants.voiceStr())).save()
      order.id
    }

    val Some(result) = route(FakeRequest(GET, getEgraph(orderId).url))

    status(result) should be (OK)
  }
  
  private def egraphRequestAsCustomer(orderId: Long, customerId: Option[Long]): Result = {
    val requestBase = FakeRequest(GET, getEgraph(orderId).url)
    val req = customerId.map(id => requestBase.withCustomer(id)).getOrElse(requestBase)

    route(req).get
  }  
}
