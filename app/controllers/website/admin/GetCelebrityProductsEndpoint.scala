package controllers.website.admin

import play.mvc.Controller
import services.http.AdminRequestFilters

private[controllers] trait GetCelebrityProductsEndpoint {

  this: Controller =>

  protected def adminFilters: AdminRequestFilters

  def getCelebrityProducts = {
    adminFilters.requireCelebrity {
      (celebrity) =>
        views.Application.html.admin_celebrityproducts(celebrity = celebrity, products = celebrity.products())
    }
  }
}
