package controllers.website.admin

import play.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers
import services.Utils

private[controllers] trait GetOrdersEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters

  def getOrders(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        var query = filter match {
          case "rejected" => orderStore.findByFilter(orderQueryFilters.rejected)
          case "signerActionable" => orderStore.findByFilter(orderQueryFilters.actionableOnly: _*)
          case "all" => orderStore.findByFilter()
          case _ => orderStore.findByFilter(orderQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[Order], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetOrdersEndpoint.url())
        views.Application.admin.html.admin_orders(orders = pagedQuery._1)
    }
  }
}

object GetOrdersEndpoint {

  def url(): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getOrders")
  }
}
