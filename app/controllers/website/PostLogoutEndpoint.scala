package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import services.http.{SecurityRequestFilters, ControllerMethod}

private[controllers] trait PostLogoutEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters

  def postLogout() = controllerMethod() {
    securityFilters.checkAuthenticity{
      session.clear()
      new Redirect(Utils.lookupUrl("WebsiteControllers.getRoot").url)
    }
  }
}
