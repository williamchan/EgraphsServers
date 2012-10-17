package controllers.website.admin

import play.api.mvc.Controller
import models._
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory

private[controllers] trait GetOrdersAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore
  
  import services.AppConfig.instance
  private def orderQueryFilters = instance[OrderQueryFilters]

  def getOrdersAdmin(filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val query = filter match {
          case "rejectedByAdmin" => orderStore.findByFilter(orderQueryFilters.rejectedByAdmin)
          case "rejectedByCelebrity" => orderStore.findByFilter(orderQueryFilters.rejectedByCelebrity)
          case "signerActionable" => orderStore.findByFilter(orderQueryFilters.actionableOnly: _*)
          case "all" => orderStore.findByFilter()
          case _ => orderStore.findByFilter(orderQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[Order], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
        Ok(views.html.Application.admin.admin_orders(orders = pagedQuery._1))
      }
    }
  }
}

object GetOrdersAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getOrdersAdmin().url
  }
}
