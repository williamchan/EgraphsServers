package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID

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

    val fbState = UUID.randomUUID().toString
    session.put(Facebook._fbState, fbState)
    val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState)
    views.Application.html.register(errorFields = errorFields, fields = fieldDefaults, fbOauthUrl = fbOauthUrl)
  }

}

object GetRegisterEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getRegister")
  }
}