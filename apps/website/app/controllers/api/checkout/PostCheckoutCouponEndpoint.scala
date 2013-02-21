package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters


/** POST /sessions/[SessionID]/checkouts/[CelebID]/coupon */
trait PostCheckoutCouponEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters



  //
  // Controllers
  //
  /** Returns Ok if the coupon code is valid and the corresponding coupon is successfully added to the checkout */
  def postCheckoutCoupon(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

      /**
       * TODO(CE-13): Bind request to coupon form, add the LIT to the checkout, recache, and return Ok.
       * Return BadRequest if form binding fails or the coupon code is invalid.
       */

        Ok
      }
//  }
  }

}
