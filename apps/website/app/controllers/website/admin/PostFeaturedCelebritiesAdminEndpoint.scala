package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action

trait PostFeaturedCelebritiesAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore

  /**
   * POSTs a new list of celebrity IDs to be featured.
   * @param celebIds the new set of celebrity IDs to feature
   *
   * @return a Redirect to the celebrities admin endpoint.
   */
  def postFeaturedCelebrities = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
         val celebIds = request.body.asFormUrlEncoded match {
          case Some(params) if(params.contains("celebIds")) => {
            for(celebId <- params("celebIds")) yield {
              celebId.toLong
            }
          }
          case _ => List[Long]()
        }
        celebrityStore.updateFeaturedCelebrities(celebIds)
        Redirect(GetCelebritiesAdminEndpoint.location)
      }
    }
  }
}
