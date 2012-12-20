package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import play.api.data._
import play.api.data.Forms._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}
import models.categories.Featured
import models.frontend.admin.CelebrityAdminViewModel

private[controllers] trait GetCelebritiesAdminEndpoint extends ImplicitHeaderAndFooterData  {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod
  protected def featured: Featured

  def getCelebritiesAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        // get query parameters
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)

        val query = celebrityStore.getCelebrityAccounts
        val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)

        // get all celebrities and mark the featured ones
        val featuredCelebrities = celebrityStore.marketplaceSearch(refinements = List(List(featured.categoryValue.id))).toList
        val featuredMap = featuredCelebrities.groupBy(celebrity => celebrity.id) // should only have one for each group
        val celebrities = celebrityStore.getAll.map(celebrity =>
          CelebrityAdminViewModel(
            id = celebrity.id,
            publicName = celebrity.publicName,
            isFeatured = featuredMap.contains(celebrity.id))
        )
        Ok(views.html.Application.admin.admin_celebrities(
          celebrityAccounts = pagedQuery._1,
          allCelebrities = celebrities // for the Featured Stars chooser
        ))
      }
    }
  }

  /**
   * Return celebrities that match the text query. Full text search on publicname and roledescription.
   * See reference here: http://www.postgresql.org/docs/9.2/interactive/textsearch-controls.html
   *
   * @param query user inputted string
   * @return CelebrityListings of matching result.
   */
  def getCelebritiesBySearchAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val query = Form("query" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        val listings = celebrityStore.search(query=query)
        Ok(views.html.Application.admin.admin_celebrities_search(celebrityListings = listings, query = query))
      }
    }
  }
}

object GetCelebritiesAdminEndpoint {
  def location = {
    controllers.routes.WebsiteControllers.getCelebritiesAdmin.url
  }
}
