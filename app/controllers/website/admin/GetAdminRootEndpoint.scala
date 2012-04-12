package controllers.website.admin

import play.mvc.Controller
import services.Utils
import play.mvc.results.Redirect
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetAdminRootEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getAdminRoot = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      new Redirect(GetCelebritiesEndpoint.url().url)
    }
  }
}

object GetAdminRootEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getAdminRoot")
  }
}