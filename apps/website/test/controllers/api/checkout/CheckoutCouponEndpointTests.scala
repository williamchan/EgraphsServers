package controllers.api.checkout

import utils.{DBTransactionPerTest, EgraphsUnitTest, ClearsCacheBefore, TestData}
import play.api.test.FakeRequest
import utils.FunctionalTestUtils._
import play.api.test.Helpers._
import play.api.mvc.Result
import services.db.{TransactionSerializable, DBSession}
import services.AppConfig
import models.{Coupon, Celebrity}
import play.api.libs.json._
import controllers.routes.ApiControllers.{postCheckoutCoupon, getCheckoutCoupon}

class CheckoutCouponEndpointTests extends EgraphsUnitTest with ClearsCacheBefore {
  import CheckoutCouponEndpointHelper._
  def db = AppConfig.instance[DBSession]

  "Getting a coupon when none had previously been posted" should "404" in new EgraphsTestApplication {
    status(getCoupon("ouahefiaef", makeCeleb().id)) should be (NOT_FOUND)
  }

  "Getting a coupon when one had previously been posted" should "provide the coupon form as json" in new EgraphsTestApplication {
    val celeb = makeCeleb()

    val sessionId = TestData.generateSessionId()
    val couponCode = "iajefieaj"
    status(postCoupon(couponCode, sessionId, celeb.id)) should be (BAD_REQUEST)

    val result = getCoupon(sessionId, celeb.id)
    status(result) should be (OK)
    (Json.parse(contentAsString(result)) \ "couponCode").asOpt[String] should be (Some(couponCode))
  }

  "Posting an invalid coupon" should "return error json" in new EgraphsTestApplication {
    val celeb = makeCeleb()

    val sessionId = TestData.generateSessionId()
    val couponCode = "iajefieaj"
    val response = postCoupon(couponCode, sessionId, celeb.id)
    status(response) should be (BAD_REQUEST)
    val json = Json.parse(contentAsString(response))
    val jsonErrors = json \ "errors"
    (jsonErrors \ "couponCode") should be (Json.toJson(Seq("invalid_code")))
  }

  "Getting a coupon from a different celeb than the one that had been posted" should "404" in new EgraphsTestApplication {
    val celeb1 = makeCeleb()
    val celeb2 = makeCeleb()

    val sessionId = TestData.generateSessionId()
    postCoupon("aouefnj", sessionId, celeb1.id)
    status(getCoupon(sessionId, celeb2.id)) should be (NOT_FOUND)
  }



  private def makeCeleb(): Celebrity = {
    db.connected(TransactionSerializable)(TestData.newSavedCelebrity())
  }
}

object CheckoutCouponEndpointHelper {

  def postCoupon(couponCode: String, sessionId: String, celebId: Long) = {
    val req = FakeRequest(POST, postCheckoutCoupon(sessionId, celebId).url)
      .withFormUrlEncodedBody("couponCode" -> couponCode)
      .withSessionId(sessionId)
    route(req).get
  }

  def getCoupon(sessionId: String, celebId: Long): Result = {
    val req = FakeRequest(GET, getCheckoutCoupon(sessionId, celebId).url).withSessionId(sessionId)
    route(req).get
  }
}