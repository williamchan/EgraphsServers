package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters


/** POST /sessions/[SessionID]/checkouts/[CelebID]/shipping-address */
trait PostCheckoutShippingAddressEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters



  //
  // Controllers
  //
  /** Returns Ok if the shipping address binds and is successfully added to the checkout */
  def postCheckoutShippingAddress(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

      /**
       * TODO(CE-13): Bind request to shipping address form, add the LIT to the checkout, recache, and return Ok.
       * Return BadRequest if form binding fails (or no shipping address is needed for the checkout?).
       */

        Ok
      }
//  }
  }

}

