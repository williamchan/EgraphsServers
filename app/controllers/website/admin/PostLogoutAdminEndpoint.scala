package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import services.http.{SecurityRequestFilters, ControllerMethod}

private[controllers] trait PostLogoutAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters

  def postLogoutAdmin() = controllerMethod() {
    securityFilters.checkAuthenticity{
      session.clear()
      new Redirect(Utils.lookupUrl("WebsiteControllers.getLoginAdmin").url)
    }
  }

}
