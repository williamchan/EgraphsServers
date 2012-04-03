package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import services.http.ControllerMethod

private[controllers] trait PostAdminLogoutEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  def postAdminLogout() = controllerMethod() {
    session.clear()
    new Redirect(Utils.lookupUrl("WebsiteControllers.getAdminLogin").url)
  }

}
