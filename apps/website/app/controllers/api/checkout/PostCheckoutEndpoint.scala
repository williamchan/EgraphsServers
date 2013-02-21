package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters


/** POST /sessions/[SessionID]/checkouts/[CelebID]?validate */
trait PostCheckoutEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters



  //
  // Controllers
  //
  /** Returns Ok if the checkout is ready to attempt transaction */
  def postCheckoutValidate(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

        /**
         * TODO(CE-13): Get checkout from cache and check that it is ready to validate.
         * If it is, return Ok, otherwise BadRequest with json body specifying missing resources.
         */

        Ok
      }
//  }
  }

}
