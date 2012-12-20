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

  it should "be accessible only to buyer or recipient of order" in new EgraphsTestApplication {
    val (orderId, buyer, recipient, anotherCustomer) = db.connected(TransactionSerializable) {
      val buyer = TestData.newSavedCustomer()
      val recipient = TestData.newSavedCustomer()
      val anotherCustomer = TestData.newSavedCustomer()
      val order = buyer.buy(TestData.newSavedProduct(), recipient = recipient).save()
      (order.id, buyer, recipient, anotherCustomer)
    }

    val requestAsCustomer: Option[Long] => Result = getEgraphExplanationCardRequestAsCustomer(orderId, _)

    // random customers are forbidden
    status(requestAsCustomer(Some(anotherCustomer.id))) should be(FORBIDDEN)

    // buyer and recipient are permitted
    status(requestAsCustomer(Some(buyer.id))) should be(OK)
    status(requestAsCustomer(Some(recipient.id))) should be(OK)

    // random customers are redirected away
    status(requestAsCustomer(None)) should be(SEE_OTHER)
  }

  private def getEgraphExplanationCardRequestAsCustomer(orderId: Long, customerId: Option[Long]): Result = {
    val req = customerId.map(id => FakeRequest().withCustomer(id)).getOrElse(FakeRequest())
    routeAndCall(req.copy(method = GET, uri = getEgraphExplanationCard(orderId).url)).get
  }
}
