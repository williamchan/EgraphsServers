package controllers.website.admin

import play.api.mvc.Controller
import models._
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCelebrityProductsAdminEndpoint extends ImplicitHeaderAndFooterData {

  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCelebrityProductsAdmin(celebrityId: Long, page: Int = 1) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>
          val query = celebrity.products()
          val pagedQuery: (Iterable[Product], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
          implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
          Ok(views.html.Application.admin.admin_celebrityproducts(celebrity = celebrity, products = pagedQuery._1))
        }
      }
    }
  }
}

object GetCelebrityProductsAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCelebrityProductsAdmin(celebrityId = celebrity.id).url
  }
}
