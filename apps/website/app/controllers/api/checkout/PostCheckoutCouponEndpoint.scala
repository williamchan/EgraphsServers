package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import models.checkout.forms.CouponForm
import models.checkout.CheckoutAdapterServices


/** POST /sessions/[SessionID]/checkouts/[CelebID]/coupon */
trait PostCheckoutCouponEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def checkoutAdapters: CheckoutAdapterServices

  //
  // Controllers
  //
  /** Returns Ok if the coupon code is valid and the corresponding coupon is successfully added to the checkout */
  def postCheckoutCoupon(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
    httpFilters.requireSessionAndCelebrityUrlSlugs(sessionIdSlug, checkoutIdSlug) { (sessionId, celeb) =>
      Action { implicit request =>
        val checkout = checkoutAdapters.decacheOrCreate(celeb.id)

        CouponForm.bindFromRequestAndCache(checkout).fold(
          formWithErrors => {
            checkout.withCoupon(None).cache()
            BadRequest(formWithErrors.errorsAsJson)
          },

          couponType => {
            checkout.withCoupon(Some(couponType)).cache()
            Ok
          }
        )
      }
    }
  }
}
