package controllers.website.admin

import play.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers
import services.Utils

private[controllers] trait GetCelebrityOrdersAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def celebrityStore: CelebrityStore
  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters

  def getCelebrityOrdersAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity, admin) =>
        val query = filter match {
          case "rejectedByAdmin" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.rejectedByAdmin)
          case "rejectedByCelebrity" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.rejectedByCelebrity)
          case "signerActionable" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.actionableOnly: _*)
          case "all" => orderStore.findByCelebrity(celebrityId = celebrity.id)
          case _ => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[Order], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityOrdersAdminEndpoint.url(celebrity = celebrity), filter = Some(filter))
        views.Application.admin.html.admin_orders(orders = pagedQuery._1, celebrity = Some(celebrity))
    }
  }
}

object GetCelebrityOrdersAdminEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityOrdersAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}
