package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod

private[controllers] trait GetLoginEndpoint {
  this: Controller =>

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod

  def getLogin = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val action = Utils.lookupAbsoluteUrl("WebsiteControllers.postFacebookLoginCallback")
    val callbackUrl = action.url

    views.Application.html.login(
      errorFields = errorFields,
      fbAppId = facebookAppId,
      callbackUrl = callbackUrl
    )
  }

}

object GetLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLogin")
  }
}