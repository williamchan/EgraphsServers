package controllers.website.consumer

import play.api.mvc.{Action, AnyContent, Controller}
import services.http.filters.HttpFilters
import services.http.{ControllerMethod, POSTControllerMethod}

trait PurchaseFlowPersonalizationEndpoints
// extends ???
{ this: Controller =>


  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  /* declare additional needed services */



  //
  // Controllers
  //
  /**
   * TODO(CE-13): Serve personalize page.
   * Similar to `StorefrontPersonalizeConsumerEndpoints#getStorefrontPersonalize`, but doesn't need product url slug.
   */
  def getPersonalize(celebrityUrlSlug: String, productUrlSlug: String): Action[AnyContent] =
  controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
      Action { implicit request =>

        /**
         * return view with form, or redirect somewhere to sensible on error
         */

        Ok
      }
    }
  }
}
