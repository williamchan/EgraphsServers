package controllers.website.example

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import play.Play

private[controllers] trait GetSocialPostEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  private def fbAppIdKey = "fb.appid"

  def getSocialPost = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        val fbAppId = Play.configuration.getProperty(fbAppIdKey)
        views.Application.example.html.socialpost(fbAppId = fbAppId)
    }
  }
}