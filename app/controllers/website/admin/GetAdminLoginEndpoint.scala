package controllers.website.admin

import models.CelebrityStore
import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod

private[controllers] trait GetAdminLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore

  def getAdminLogin = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    views.Application.admin.html.admin_login(errorFields = errorFields)
  }

}

object GetAdminLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getAdminLogin")
  }
}