package controllers.website

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetSocialTestEndpoint { this: Controller =>
  import views.Application._

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getSocialTest = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      html.socialtest()
    }
  }
}