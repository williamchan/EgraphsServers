package controllers.website.example

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetSocialPostEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getSocialPost = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        views.Application.example.html.socialpost()
    }
  }
}