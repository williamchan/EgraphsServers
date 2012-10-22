package controllers.website.admin

import models._
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetPrintOrdersAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def printOrderStore: PrintOrderStore

  import services.AppConfig.instance
  private def printOrderQueryFilters = instance[PrintOrderQueryFilters]
    
  def getPrintOrdersAdmin(filter: String = "unfulfilled", page: Int = 1) = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val query = filter match {
          case "unfulfilled" => printOrderStore.findByFilter(printOrderQueryFilters.unfulfilled)
          case "hasEgraphButLacksPng" => printOrderStore.findHasEgraphButLacksPng()
          case "hasPng" => printOrderStore.findByFilter(printOrderQueryFilters.hasPng)
          case "fulfilled" => printOrderStore.findByFilter(printOrderQueryFilters.fulfilled)
          case "all" => printOrderStore.findByFilter()
          case _ => printOrderStore.findByFilter(printOrderQueryFilters.unfulfilled)
        }
        val pagedQuery: (Iterable[(PrintOrder, Order, Option[Egraph])], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
        Ok(views.html.Application.admin.admin_printorders(printOrderDate = pagedQuery._1))
      }
    }
  }
}

object GetPrintOrdersAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getPrintOrdersAdmin().url
  }
}