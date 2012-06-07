package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import services.http.{POSTControllerMethod}

private[controllers] trait PostLogoutAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod

  def postLogoutAdmin() = postController() {
    session.clear()
    new Redirect(Utils.lookupUrl("WebsiteControllers.getLoginAdmin").url)
  }
}
