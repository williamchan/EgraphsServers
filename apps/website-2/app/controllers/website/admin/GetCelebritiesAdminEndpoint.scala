package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory


private[controllers] trait GetCelebritiesAdminEndpoint {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebritiesAdmin(page: Int = 1) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      Action { implicit request =>
        val query = celebrityStore.getCelebrityAccounts
        val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        val listings = for((celebrity: Celebrity, account: Account) <- pagedQuery._1) yield {
          Celebrity.celebrityAccountToListing(celebrity, account)
        }
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)
        Ok(views.html.Application.admin.admin_celebrities(
          celebrityListings = listings,
          allCelebrities = celebrityStore.getAll
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
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      Action { implicit request =>
        //TODO(play2 wchan)
        val query = ""
        val listings = celebrityStore.findByTextQuery(query)
        Ok(views.html.Application.admin.admin_celebrities_search(celebrityListings = listings))
      }
    }
  }
}

object GetCelebritiesAdminEndpoint {

  def location = {
    controllers.routes.WebsiteControllers.getCelebritiesAdmin().url
  }
}
