package controllers.website.consumer

import controllers.routes.WebsiteControllers.getEgraphExplanationCard
import play.api.mvc.Result
import play.api.test._
import play.api.test.Helpers._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import utils.{EgraphsUnitTest, FunctionalTestUtils, TestData}
import FunctionalTestUtils.Conversions._

class GetEgraphExplanationCardEndpointTests extends EgraphsUnitTest {

  private def db = AppConfig.instance[DBSession]

  it should "return a pdf in the happy path" in new EgraphsTestApplication {
    val (orderId, customer) = db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder()
      (order.id, order.buyer)
    }

    val response = getEgraphExplanationCardRequestAsCustomer(orderId, Some(customer.id))
    contentType(response) should be(Some("application/pdf"))
    contentAsBytes(response).length should be > (800000)
  }

  it should "be inaccessible to random visitors" in new EgraphsTestApplication {
    val (orderId, buyer, recipient, anotherCustomer, admin) = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val recipient = TestData.newSavedCustomer()
      val anotherCustomer = TestData.newSavedCustomer()
      val order = buyer.buyUnsafe(TestData.newSavedProduct(), recipient = recipient).save()
      val admin = TestData.newSavedAdministrator()
      (order.id, buyer, recipient, anotherCustomer, admin)
    }

    val requestAsCustomer: Option[Long] => Result = getEgraphExplanationCardRequestAsCustomer(orderId, _)

    // random customers are forbidden
    status(requestAsCustomer(Some(anotherCustomer.id))) should be(FORBIDDEN)
    status(requestAsCustomer(None)) should be(FORBIDDEN)

    // buyer and recipient and admins are permitted
    status(requestAsCustomer(Some(buyer.id))) should be(OK)
    status(requestAsCustomer(Some(recipient.id))) should be(OK)
    
    val adminReq = FakeRequest(GET, getEgraphExplanationCard(orderId).url).withAdmin(admin.id)
    status(route(adminReq).get) should be (OK)
  }

  private def getEgraphExplanationCardRequestAsCustomer(orderId: Long, customerId: Option[Long]): Result = {
    val baseRequest = FakeRequest(GET, getEgraphExplanationCard(orderId).url)
    val req = customerId.map(id => baseRequest.withCustomer(id)).getOrElse(baseRequest)
    route(req).get
  }
}
