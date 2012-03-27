package controllers.website.admin

import play.mvc.Controller
import services.http.AdminRequestFilters
import models._

private[controllers] trait GetCelebrityOrdersEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters

  protected def celebrityStore: CelebrityStore
  protected def orderStore: OrderStore

  def getCelebrityOrders =  {
    adminFilters.requireCelebrity {
      (celebrity) =>
        val orders: Iterable[Order] = orderStore.findByCelebrity(celebrityId = celebrity.id)
        views.Application.html.admin_celebrityorders(celebrity, orders)
    }
  }
}