package controllers.api.checkout

import play.api.mvc.{Result, Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import models.checkout.forms.{CheckoutForm, CouponForm}
import models.checkout.{CouponLineItemType, EgraphCheckoutAdapter, CheckoutAdapterServices}

/** POST /sessions/[SessionID]/checkouts/[CelebID]/coupon */
trait CheckoutCouponEndpoints extends CheckoutResourceEndpoint[CouponLineItemType] { this: Controller =>
  //
  // CheckoutResourceEndpoint members
  //
  override protected def resourceForm = CouponForm

  override protected def setResource(
    resource: Option[CouponLineItemType],
    checkout: EgraphCheckoutAdapter
  ) = {
    checkout.withCoupon(resource)
  }

  /** Returns Ok if the coupon code is valid and the corresponding coupon is successfully added to the checkout */
  def postCheckoutCoupon(sessionId: UrlSlug, checkoutId: Long): Action[AnyContent] = {
    postCheckoutResource(sessionId, checkoutId)
  }

  def getCheckoutCoupon(sessionIdSlug: UrlSlug, checkoutId: Long): Action[AnyContent] = {
    getCheckoutResource(sessionIdSlug, checkoutId)
  }
}

