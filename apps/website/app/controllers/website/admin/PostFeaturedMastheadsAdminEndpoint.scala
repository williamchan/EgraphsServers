package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import categories.Featured

trait PostFeaturedMastheadsAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def featured: Featured

  /**
   * POSTs a new list of masthead IDs to be featured.
   * @param mastheadIds the new set of celebrity IDs to feature
   *
   * @return a Redirect to the mastheads admin endpoint.
   */
  def postFeaturedMastheads = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val mastheadIds = request.body.asFormUrlEncoded match {
          case Some(params) if(params.contains("mastheadIds")) => {
            for(mastheadId <- params("mastheadIds")) yield {
             mastheadId.toLong
            }
          }
          case None => List[Long]()
        }
        featured.updateFeaturedMastheads(mastheadIds)
        Redirect(controllers.routes.WebsiteControllers.getMastheadsAdmin().url)
      }
    }
  }
}
