package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import utils.{AdminProtectedResourceTests, EgraphsUnitTest, FunctionalTestUtils}
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import models.{Coupon, CouponStore}
import models.enums.{CouponType, CouponDiscountType, CouponUsageType}

class PostCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.postCouponAdmin(1)
}

class PostCreateCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  import controllers.routes.WebsiteControllers.postCreateCouponAdmin
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = postCreateCouponAdmin
  private def couponStore = AppConfig.instance[CouponStore]
  
  routeName(postCreateCouponAdmin) should "create coupon in happy path" in new EgraphsTestApplication {
    val result = performRequest()
    val location = redirectLocation(result).getOrElse("")
    location should startWith("/admin/coupons/")
    val couponId = FunctionalTestUtils.extractId(location)
    db.connected(TransactionSerializable) {
      couponStore.findById(couponId) should not be(None)
    }
  }
  
  routeName(postCreateCouponAdmin) should "validate fields" in new EgraphsTestApplication {
    val trainwreck = performRequest(
      startDate = "clearlynotadate",
      endDate = "clearlynotadate",
      couponTypeString = "notacoupontype",
      discountTypeString = "notadiscounttype",
      usageTypeString = "notausagetype"
    )
    val errors = flash(trainwreck).get("errors").getOrElse("")
    errors should include("startDate: Date was not correctly formatted")
    errors should include("endDate: Date was not correctly formatted")
    errors should include("couponTypeString: Error setting coupon type, please contact support")
    errors should include("discountTypeString: Error setting discount type, please contact support")
    errors should include("usageTypeString: Error setting coupon usage type, please contact support")
  }
  
  routeName(postCreateCouponAdmin) should "validate coupon is unique" in new EgraphsTestApplication {
    val usedCode = db.connected(TransactionSerializable) { Coupon().save().code }
    val trainwreck = performRequest(code = usedCode)
    val errors = flash(trainwreck).get("errors").getOrElse("")
    errors should include("An active coupon with that code already exists.")
  }
  
  private def performRequest(
      name: String = "",
      code: String = Coupon().code, 
      startDate: String = "2012-01-01 00:00",
      endDate: String = "2022-01-01 00:00",
      discountAmount: Int = 5,
      couponTypeString: String = CouponType.Promotion.name,
      discountTypeString: String = CouponDiscountType.Flat.name,
      usageTypeString: String = CouponUsageType.OneUse.name,
      restrictions: String = "{}"
  ): play.api.mvc.Result = {
    controllers.WebsiteControllers.postCreateCouponAdmin(
      FakeRequest().withAdmin(admin.id).withFormUrlEncodedBody(
          "name" -> name,
    	  "code" -> code,
    	  "startDate" -> startDate,
    	  "endDate" -> endDate,
    	  "discountAmount" -> discountAmount.toString,
    	  "couponTypeString" -> couponTypeString,
    	  "discountTypeString" -> discountTypeString,
    	  "usageTypeString" -> usageTypeString,
    	  "restrictions" -> restrictions
      ).withAuthToken
    )
  }
}
