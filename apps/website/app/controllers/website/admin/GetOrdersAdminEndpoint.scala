package controllers.website.admin

import models._
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory

private[controllers] trait GetOrdersAdminEndpoint extends ImplicitHeaderAndFooterData  {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters

  def getOrdersAdmin = controllerMethod.withForm() 
  { implicit authToken => 
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        
        // get query parameters
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val filter: String = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "pendingAdminReview", validForm => validForm)
        
        val query = filter match {
          case "rejectedByAdmin" => orderStore.getOrderResults(orderQueryFilters.rejectedByAdmin)
          case "rejectedByCelebrity" => orderStore.getOrderResults(orderQueryFilters.rejectedByCelebrity)
          case "signerActionable" => orderStore.getOrderResults(orderQueryFilters.actionableOnly: _*)
          case "all" => orderStore.getOrderResults()
          case _ => orderStore.getOrderResults(orderQueryFilters.pendingAdminReview)
        }
        val pagedQuery: (Iterable[(Order, Celebrity)], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url, filter = Option(filter))
        Ok(views.html.Application.admin.admin_orders(orderResults = pagedQuery._1))
      }
    }
  }
}

object GetOrdersAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getOrdersAdmin.url
  }
}
