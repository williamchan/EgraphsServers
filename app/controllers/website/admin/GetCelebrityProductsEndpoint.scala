package controllers.website.admin

import play.mvc.Controller
import services.http.{ControllerMethod, AdminRequestFilters}

private[controllers] trait GetCelebrityProductsEndpoint {

  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getCelebrityProducts = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity) =>
        views.Application.admin.html.admin_celebrityproducts(celebrity = celebrity, products = celebrity.products())
    }
  }
}
