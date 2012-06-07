package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod

private[controllers] trait GetRegisterEndpoint {
  this: Controller =>

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod

  def getRegister = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "email" => flash.get("email")
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }

    val action = Utils.lookupAbsoluteUrl("WebsiteControllers.postFacebookLoginCallback")
    val callbackUrl = action.url

    views.Application.html.register(
      errorFields = errorFields,
      fields = fieldDefaults,
      fbAppId = facebookAppId,
      callbackUrl = callbackUrl
    )
  }

}

object GetRegisterEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getRegister")
  }
}