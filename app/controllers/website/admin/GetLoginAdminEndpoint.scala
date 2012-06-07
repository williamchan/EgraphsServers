package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod

private[controllers] trait GetLoginAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  def getLoginAdmin = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    views.Application.admin.html.admin_login(errorFields = errorFields)
  }

}

object GetLoginAdminEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLoginAdmin")
  }
}