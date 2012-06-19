package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers

private[controllers] trait GetCelebritiesAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebritiesAdmin(page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val query = celebrityStore.getCelebrityAccounts
      val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
      WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)
      views.Application.admin.html.admin_celebrities(
        celebrityAccounts = pagedQuery._1,
        celebrityStore.getAll // for the Featured Stars chooser
      )
    }
  }
}

object GetCelebritiesAdminEndpoint {

  def location: ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin")
  }
}
