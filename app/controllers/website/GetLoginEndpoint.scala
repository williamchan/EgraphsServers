package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import play.Play

private[controllers] trait GetLoginEndpoint {
  this: Controller =>

  private def fbAppIdKey = "fb.appid"
  protected def controllerMethod: ControllerMethod

  def getLogin = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val fbAppId = Play.configuration.getProperty(fbAppIdKey)
    val action = Utils.lookupAbsoluteUrl("WebsiteControllers.postFacebookLoginCallback")
    val callbackUrl = action.url

    views.Application.html.login(errorFields = errorFields, fbAppId = fbAppId, callbackUrl = callbackUrl)
  }

}

object GetLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLogin")
  }
}