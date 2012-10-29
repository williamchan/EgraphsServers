package controllers.website.admin

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import services.http.filters.HttpFilters
import services.mvc.ImplicitHeaderAndFooterData
import models.filters._
import controllers.PaginationInfoFactory

private[controllers] trait GetFiltersAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def filterStore: FilterStore  

  def getFiltersAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val query = filterStore.getFilters
        val pagedQuery: (Iterable[Filter], Int, Option[Int]) = services.Utils.pagedQuery(select=query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = "")
        Ok(views.html.Application.admin.admin_filters(filters=pagedQuery._1))
      }
    }
  }
}