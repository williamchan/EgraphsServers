package controllers.website.admin

import models._
import play.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}

trait PostFeaturedCelebritiesAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore

  def postFeaturedCelebrities(celebIds: Array[Long]) = postController() {
    adminFilters.requireAdministratorLogin { admin =>
      celebrityStore.updateFeaturedCelebrities(celebIds)
      Redirect(GetCelebritiesAdminEndpoint.location.url)
    }
  }
}
