package controllers.api.checkout

import utils.{TestData, ClearsCacheBefore, EgraphsUnitTest}
import utils.FunctionalTestUtils._
import services.db.{TransactionSerializable, DBSession}
import services.AppConfig
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.mvc.Result
import play.api.test.FakeRequest
import models.enums.{CheckoutCodeType, LineItemNature}
import models.checkout.{EgraphCheckoutAdapter, LineItemTestData}


class CheckoutEndpointTests extends EgraphsUnitTest with ClearsCacheBefore {
  import controllers.routes.ApiControllers.{postCheckout, getCheckout}
  def db = AppConfig.instance[DBSession]

  /** NOTE: I recall discussing this creating instead of 404ing, but API spec says to 404 -- easy to change if we want */
  "Getting a checkout for the first time" should "404" in new EgraphsTestApplication {
    val sessionId = TestData.generateSessionId()
    status(performGet(sessionId, makeCeleb().id)) should be (NOT_FOUND)
  }

  "Getting a checkout that's had resources posted" should "200 with summary" in new EgraphsTestApplication {
    val (sessionId, celeb) = sessionAndCelebAfterPostingRealCoupon()
    val result = performGet(sessionId, celeb.id)
    val contentJs = Json.parse(contentAsString(result))

    status(result) should be (OK)

    (contentJs \ LineItemNature.Discount.name.toLowerCase) match {
      case discounts: JsArray =>
        val codeTypeJs = (discounts(0) \ "lineItemType" \ "codeType")
        codeTypeJs.asOpt[String] should be (Some(CheckoutCodeType.Coupon.name))
      case other => fail(s"unexpected discounts in summary: ${Json.stringify(other)}")
    }
  }

  "Posting a nonexistent checkout" should "404" in new EgraphsTestApplication {
    val (sessionId, celeb) = sessionAndCeleb()
    status(performPost(sessionId, celeb.id)) should be (NOT_FOUND)
  }

  "Posting a checkout without an Egraph" should "403" in new EgraphsTestApplication {
    val (sessionId, celeb) = sessionAndCelebAfterPostingRealCoupon()
    val result = performPost(sessionId, celeb.id)
    status(result) should be (BAD_REQUEST)
  }

  "Posting a checkout with egraph, buyer & payment" should "200 with confirmation url" in new EgraphsTestApplication {
    import LineItemTestData._
    import controllers.routes.WebsiteControllers.getOrderConfirmation
    val (sessionId, celeb) = sessionAndCeleb()
    val checkoutAdapter = make {
      EgraphCheckoutAdapter(celeb.id)
        .withOrder( Some(randomEgraphOrderType()) )
        .withPayment( Some(randomCashTransactionType) )
        .withBuyerEmail( Some(TestData.generateEmail()) )
        .cache()(postRequest(sessionId, celeb.id))
    }

    val result = performPost(sessionId, celeb.id)
    val content = Json.parse(contentAsString(result))
    val url = (content \ "order" \ "confirmationUrl").as[String]
    status(result) should be (OK)
    url.filterNot(_.isDigit) should be (getOrderConfirmation(0).url.filterNot(_.isDigit))
  }


  //
  // Helpers
  //
  def performGet(sessionId: String, celebId: Long): Result = route(getRequest(sessionId, celebId)).get
  def performPost(sessionId: String, celebId: Long): Result = route(postRequest(sessionId, celebId)).get

  private def getRequest(sessionId: String, celebId: Long) = {
    FakeRequest(GET, getCheckout(sessionId, celebId).url).withSessionId(sessionId)
  }

  private def postRequest(sessionId: String, celebId: Long) = {
    FakeRequest(POST, postCheckout(sessionId, celebId).url).withSessionId(sessionId)
  }

  private def makeCeleb() = make(TestData.newSavedCelebrity())
  private def makeCoupon() = make(TestData.newSavedCoupon())
  private def sessionAndCeleb() = (TestData.generateSessionId(), makeCeleb())
  private def sessionAndCelebAfterPostingRealCoupon() = {
    val (sessionId, celeb) = sessionAndCeleb()
    val result = CheckoutCouponEndpointHelper.postCoupon(makeCoupon().code, sessionId, celeb.id)
    status(result) should be (OK)
    (sessionId, celeb)
  }

  /** wraps data maker in db connection */
  private def make[T](factory: => T): T = db.connected(TransactionSerializable)(factory)
}
