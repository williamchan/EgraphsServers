package controllers.api.checkout

import play.api.mvc.{Result, Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import models.checkout.forms.{CheckoutForm, CouponForm}
import models.checkout.{CouponLineItemType, EgraphCheckoutAdapter, CheckoutAdapterServices}


/** POST /sessions/[SessionID]/checkouts/[CelebID]/coupon */
trait PostCheckoutCouponEndpoint extends CheckoutResourceEndpoint[CouponLineItemType] { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def checkoutAdapters: CheckoutAdapterServices

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
  def postCheckoutCoupon(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = {
    postCheckoutResource(sessionIdSlug, checkoutIdSlug)
  }

  def getCheckoutCoupon(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = {
    getCheckoutResource(sessionIdSlug, checkoutIdSlug)
  }
}
