package controllers.website.example

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import play.Play
import services.Utils

private[controllers] trait GetFacebookLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  private def fbAppIdKey = "fb.appid"

  def getFacebookLogin = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        val fbAppId = Play.configuration.getProperty(fbAppIdKey)
        val action = Utils.lookupAbsoluteUrl("WebsiteControllers.postFacebookLoginCallback")
        val callbackUrl = action.url
        views.Application.example.html.facebooklogin(fbAppId = fbAppId, callbackUrl = callbackUrl)
    }
  }
}