package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.ControllerMethod
import services.http.filters.HttpFilters


/** GET /sessions/[SessionID]/checkouts/[CelebID] */
trait GetCheckoutEndpoint { this: Controller =>

  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  //
  // Controllers
  //
  /** Returns JSON representation of the checkout */
  def getCheckout(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = controllerMethod() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

      /**
       * TODO(CE-13): Get checkout from cache and return Ok with checkout.toJson as body.
       * Return BadRequest if checkout doesn't exist.
       */

        Ok
      }
//  }
  }
}
