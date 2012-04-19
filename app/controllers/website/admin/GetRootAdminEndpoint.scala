package controllers.website.admin

import play.mvc.Controller
import services.Utils
import play.mvc.results.Redirect
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetRootAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getRootAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      new Redirect(GetCelebritiesAdminEndpoint.url().url)
    }
  }
}

object GetRootAdminEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getRootAdmin")
  }
}