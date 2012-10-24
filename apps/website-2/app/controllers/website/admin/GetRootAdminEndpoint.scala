package controllers.website.admin

import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetRootAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getRootAdmin = controllerMethod() {
    controllerMethod() {
      httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
        Action { implicit request =>
          Redirect(GetCelebritiesAdminEndpoint.location)
        }
      }
    }
  }
}

object GetRootAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getRootAdmin.url
  }
}