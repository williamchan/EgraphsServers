package controllers.website.example

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils

private[controllers] trait GetFacebookLoginEndpoint {
  this: Controller =>

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getFacebookLogin = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        val action = Utils.lookupAbsoluteUrl("WebsiteControllers.postFacebookLoginCallback")
        val callbackUrl = action.url
        views.Application.example.html.facebooklogin(
          fbAppId = facebookAppId,
          callbackUrl = callbackUrl
        )
    }
  }
}