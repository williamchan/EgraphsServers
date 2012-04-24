package controllers.website.example

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetFacebookLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getFacebookLogin = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        views.Application.example.html.facebooklogin()
    }
  }
}