package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers

private[controllers] trait GetCelebritiesEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebrities(page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      var query = celebrityStore.getCelebrityAccounts
      val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
      WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebritiesEndpoint.url())
      views.Application.admin.html.admin_celebrities(celebrityAccounts = pagedQuery._1)
    }
  }
}

object GetCelebritiesEndpoint {

  def url(): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrities")
  }
}
