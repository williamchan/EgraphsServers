package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID

private[controllers] trait GetLoginEndpoint {
  this: Controller =>

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod

  def getLogin = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val fbState = UUID.randomUUID().toString
    session.put(Facebook._fbState, fbState)
    val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState)
    views.Application.html.login(errorFields = errorFields, fbOauthUrl = fbOauthUrl)
  }

}

object GetLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLogin")
  }
}