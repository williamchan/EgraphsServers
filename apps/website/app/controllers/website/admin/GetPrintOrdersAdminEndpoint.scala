package controllers.website.admin

import play.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import controllers.WebsiteControllers
import services.Utils
import play.mvc.Router.ActionDefinition

private[controllers] trait GetPrintOrdersAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def printOrderQueryFilters: PrintOrderQueryFilters
  protected def printOrderStore: PrintOrderStore

  def getPrintOrdersAdmin(filter: String = "all", page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        val query = filter match {
          case "unfulfilled" => printOrderStore.findByFilter(printOrderQueryFilters.unfulfilled)
          case "fulfilled" => printOrderStore.findByFilter(printOrderQueryFilters.fulfilled)
          case "all" => printOrderStore.findByFilter()
          case _ => printOrderStore.findByFilter(printOrderQueryFilters.unfulfilled)
        }
        val pagedQuery: (Iterable[(PrintOrder, Order, Option[Egraph])], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetPrintOrdersAdminEndpoint.url(), filter = Some(filter))
        views.Application.admin.html.admin_printorders(printOrderDate = pagedQuery._1)
    }
  }
}

object GetPrintOrdersAdminEndpoint {

  def url(): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getPrintOrdersAdmin")
  }
}