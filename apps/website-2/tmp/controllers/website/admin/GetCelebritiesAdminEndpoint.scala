package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import play.api.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

private[controllers] trait GetCelebritiesAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebritiesAdmin(page: Int = 1) = Action { request =>
    controllerMethod() {
      adminFilters.requireAdministratorLogin { admin =>
        val query = celebrityStore.getCelebrityAccounts
        val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)
        Ok(views.html.Application.admin.admin_celebrities(
          celebrityAccounts = pagedQuery._1,
          celebrityStore.getAll // for the Featured Stars chooser
          )
        )
      }
    }
  }
}

object GetCelebritiesAdminEndpoint {

  def location = {
    controllers.routes.WebsiteControllers.getCelebritiesAdmin().url
//    WebsiteControllers.reverse(WebsiteControllers.getCelebritiesAdmin())
  }
}
