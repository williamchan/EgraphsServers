package controllers.api.checkout

import play.api.mvc.{Controller, AnyContent, Action}
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters


/** POST /sessions/[SessionID]/checkouts/[CelebID]/egraph */
trait PostCheckoutEgraphEndpoint { this: Controller =>

  //
  // Services
  //
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters



  //
  // Controllers
  //
  /** Returns Ok if the egraph LIT is successfully created and added to the checkout */
  def postCheckoutEgraph(sessionIdSlug: UrlSlug, checkoutIdSlug: UrlSlug): Action[AnyContent] = postController() {
//  httpFilter.requireCelebrityIdAndSessionIdUrlSlugs { (celebId, sessionId) =>
      Action { implicit request =>

      /**
       * TODO(CE-13): Bind request to egraph order form, add the LIT to the checkout, recache, and return Ok.
       * Return BadRequest if form binding fails, product id is invalid, or inventory is depleted.
       */

        Ok
      }
//  }
  }

}
