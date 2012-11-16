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

private[controllers] trait GetCouponsAdminEndpoint extends ImplicitHeaderAndFooterData  {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def couponStore: CouponStore
  protected def couponQueryFilters: CouponQueryFilters

  def getCouponsAdmin = controllerMethod.withForm() 
  { implicit authToken => 
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        
        // get query parameters
        val page = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val filter = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "unlimitedActive", validForm => validForm)
        
        val query = filter match {
          case "oneUseActive" => couponStore.findByFilter(
              couponQueryFilters.oneUse, couponQueryFilters.activeByDate, couponQueryFilters.activeByFlag)
          case "unlimitedActive" => couponStore.findByFilter(
              couponQueryFilters.unlimited, couponQueryFilters.activeByDate, couponQueryFilters.activeByFlag)
          case "all" => couponStore.findByFilter()
          case _ => couponStore.findByFilter(couponQueryFilters.unlimited)
        }
        
        val pagedQuery: (Iterable[Coupon], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(
            pagedQuery = pagedQuery, baseUrl = controllers.routes.WebsiteControllers.getCouponsAdmin.url, filter = Option(filter))
        Ok(views.html.Application.admin.admin_coupons(coupons = pagedQuery._1))
      }
    }
  }
}
