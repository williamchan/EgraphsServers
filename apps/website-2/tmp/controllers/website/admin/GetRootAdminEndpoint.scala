package controllers.website.admin

import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import services.http.{AdminRequestFilters, ControllerMethod}
import controllers.WebsiteControllers

private[controllers] trait GetRootAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getRootAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      new Redirect(GetCelebritiesAdminEndpoint.location.url)
    }
  }
}

object GetRootAdminEndpoint {

  def url() = {
    WebsiteControllers.reverse(WebsiteControllers.getRootAdmin)
  }
}