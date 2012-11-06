package controllers.website.admin

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import services.http.filters.HttpFilters
import services.mvc.ImplicitHeaderAndFooterData
import models.categories._
import controllers.PaginationInfoFactory

private[controllers] trait GetCategoriesAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def categoryStore: CategoryStore  

  def getCategoriesAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val query = categoryStore.getCategories
        val pagedQuery: (Iterable[Category], Int, Option[Int]) = services.Utils.pagedQuery(select=query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, 
            baseUrl = controllers.routes.WebsiteControllers.getCategoriesAdmin.url)
        Ok(views.html.Application.admin.admin_categories(categories=pagedQuery._1))
      }
    }
  }
}