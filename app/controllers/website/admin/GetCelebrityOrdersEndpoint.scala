package controllers.website.admin

import play.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import services.Utils
import controllers.WebsiteControllers

private[controllers] trait GetCelebrityOrdersEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def celebrityStore: CelebrityStore
  protected def orderStore: OrderStore

  def getCelebrityOrders(page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity) =>
        var query = orderStore.findByCelebrity(celebrityId = celebrity.id)
        val pagedQuery: (Iterable[Order], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityOrdersEndpoint.url(celebrity = celebrity))
        views.Application.admin.html.admin_celebrityorders(celebrity = celebrity, orders = pagedQuery._1)
    }
  }
}

object GetCelebrityOrdersEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityOrders", Map("celebrityId" -> celebrity.id.toString))
  }
}
