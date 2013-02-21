package controllers.website.consumer

import play.api.mvc.{Action, AnyContent, Controller}
import services.http.filters.HttpFilters
import services.http.{ControllerMethod, POSTControllerMethod}


trait PurchaseFlowCheckoutDetailsEndpoints
// extends ???
{ this: Controller =>


  //
  // Services
  //
  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  /* declare additional needed services */



  //
  // Controllers
  //
  /** TODO(CE-13): Serve checkout details page. Doesn't need product url slug. */
  def getCheckoutDetails(celebrityUrlSlug: String, productUrlSlug: String): Action[AnyContent] =
    controllerMethod.withForm() { implicit authToken =>
      httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlug, productUrlSlug) { (celeb, product) =>
        Action { implicit request =>

        /**
         * TODO(CE-13): if checkout in cache, return view with form (and json???), or redirect perhaps to personalize on error
         */

          Ok
        }
      }
    }




//  def postCheckoutDetails(celebrityUrlSlug: String, productUrlSlug: String): Action[AnyContent] = postController() {
//    httpFilters.requireCelebrityAndProductUrlSlugs(celebrityUrlSlu, productUrlSlug) { (celeb, product) =>
//      Action { implicit request =>
//
//      /**
//       * create customer
//       * create cash transaction type
//       * verify that transact is ready to happen
//       *
//       *
//       * return view for review or updates json for review or what?
//       *   (or form errors or redirect back to same page on error?)
//       */
//
//        Ok
//      }
//    }
//  }
}
