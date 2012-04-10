package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import services.http.{SecurityRequestFilters, ControllerMethod}

private[controllers] trait PostAdminLogoutEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters

  def postAdminLogout() = controllerMethod() {

    securityFilters.checkAuthenticity{
      session.clear()
      new Redirect(Utils.lookupUrl("WebsiteControllers.getAdminLogin").url)
    }
  }

}
