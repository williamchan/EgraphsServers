package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action

trait PostRebuildSearchIndexAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore

  /**
   * POSTs a call to rebuild the search index. 
   * @return a message indicating success.
   */
  def postRebuildSearchIndex = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        celebrityStore.rebuildSearchIndex
        Ok("Index has been rebuilt.")
      }
    }
  }
}
