package controllers.website.admin

import play.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}

private[controllers] trait GetCelebrityOrdersEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def celebrityStore: CelebrityStore
  protected def orderStore: OrderStore

  def getCelebrityOrders = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity) =>
        val orders: Iterable[Order] = orderStore.findByCelebrity(celebrityId = celebrity.id)
        views.Application.admin.html.admin_celebrityorders(celebrity, orders)
    }
  }
}