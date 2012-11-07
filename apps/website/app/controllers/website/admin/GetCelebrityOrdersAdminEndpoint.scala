package controllers.website.admin

import models._
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}

private[controllers] trait GetCelebrityOrdersAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters

  def getCelebrityOrdersAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>
          
          // get query parameters
          val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
          val filter: String = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "pendingAdminReview", validForm => validForm)
          
          val query = filter match {
          	case "rejectedByAdmin" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.rejectedByAdmin)
          	case "rejectedByCelebrity" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.rejectedByCelebrity)
          	case "signerActionable" => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.actionableOnly: _*)
          	case "all" => orderStore.findByCelebrity(celebrityId = celebrity.id)
          	case _ => orderStore.findByCelebrity(celebrityId = celebrity.id, orderQueryFilters.pendingAdminReview)
          }
          val pagedQuery: (Iterable[Order], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
          implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebrityOrdersAdminEndpoint.url(celebrity), filter = Option(filter))
          Ok(views.html.Application.admin.admin_orders(orders = pagedQuery._1, celebrity = Some(celebrity)))
      	}
      }
    }
  }
}

object GetCelebrityOrdersAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCelebrityOrdersAdmin(celebrityId = celebrity.id).url
  }
}
