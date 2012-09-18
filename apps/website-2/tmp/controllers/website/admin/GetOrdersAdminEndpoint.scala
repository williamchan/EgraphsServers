package controllers.website.admin

import play.api.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers
import services.Utils

private[controllers] trait GetOrdersAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters

  def getOrdersAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        val query = filter match {
          case "rejectedByAdmin" => orderStore.findByFilter(orderQueryFilters.rejectedByAdmin)
          case "rejectedByCelebrity" => orderStore.findByFilter(orderQueryFilters.rejectedByCelebrity)
          case "signerActionable" => orderStore.findByFilter(orderQueryFilters.actionableOnly: _*)
          case "all" => orderStore.findByFilter()
          case _ => orderStore.findByFilter(orderQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[Order], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url(), filter = Some(filter))
        views.html.Application.admin.admin_orders(orders = pagedQuery._1)
    }
  }
}

object GetOrdersAdminEndpoint {

  def url(): ActionDefinition = {
    WebsiteControllers.reverse(WebsiteControllers.getOrdersAdmin())
  }
}
