package controllers.website.admin

import play.mvc.Controller
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers
import models.{Product, Celebrity}

private[controllers] trait GetCelebrityProductsAdminEndpoint {

  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  def getCelebrityProductsAdmin(page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity, admin) =>
        var query = celebrity.products()
        val pagedQuery: (Iterable[Product], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityProductsAdminEndpoint.url(celebrity = celebrity))
        views.Application.admin.html.admin_celebrityproducts(celebrity = celebrity, products = pagedQuery._1)

    }
  }
}

object GetCelebrityProductsAdminEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityProductsAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}