package controllers.website.admin

import models._
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}

trait PostFeaturedCelebritiesAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore

  /**
   * POSTs a new list of celebrity IDs to be featured.
   * @param celebIds the new set of celebrity IDs to feature
   *
   * @return a Redirect to the celebrities admin endpoint.
   */
  def postFeaturedCelebrities(celebIds: Array[Long]) = postController() {
    adminFilters.requireAdministratorLogin { admin =>
      celebrityStore.updateFeaturedCelebrities(celebIds)
      Redirect(GetCelebritiesAdminEndpoint.location.url)
    }
  }
}
