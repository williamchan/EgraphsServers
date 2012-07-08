package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import services.http.POSTControllerMethod

private[controllers] trait PostLogoutEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod

  def postLogout() = postController() {
    session.clear()
    new Redirect(Utils.lookupUrl("WebsiteControllers.getRootConsumerEndpoint").url)
  }
}
