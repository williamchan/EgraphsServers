package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod

private[controllers] trait GetRecoverAccountEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  def getRecoverAccount = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    views.Application.html.recover_account(errorFields = errorFields)
  }

}

object GetRecoverAccountEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getRecoverAccount")
  }
}