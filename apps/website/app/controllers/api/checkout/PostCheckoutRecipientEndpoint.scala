package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters


/** POST /sessions/[SessionID]/checkouts/[CelebID]/recipient */
trait PostCheckoutRecipientEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters



  //
  // Controllers
  //
  /** Returns Ok if the recipient info binds and is successfully added to the checkout */
  def postCheckoutRecipient(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

      /**
       * TODO(CE-13): Bind request to customer form, add as the checkout's recipient, recache, and return Ok.
       * Return BadRequest if form binding fails.
       */

        Ok
      }
//  }
  }

}



