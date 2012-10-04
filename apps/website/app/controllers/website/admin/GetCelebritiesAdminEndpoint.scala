package controllers.website.admin

import models.{Celebrity, Account, CelebrityStore}
import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers

private[controllers] trait GetCelebritiesAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebritiesAdmin(page: Int = 1) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val query = celebrityStore.getCelebrityAccounts
      val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
      val listings = for((celebrity: Celebrity, account: Account) <- pagedQuery._1) yield {
        Celebrity.celebrityAccountToListing(celebrity, account)
      }
      WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)
      views.Application.admin.html.admin_celebrities(
        celebrityListings = listings,
        celebrityStore.getAll // for the Featured Stars chooser
      )
    }
  }

  /**
   * Return celebrities that match the text query. Full text search on  publicname and roledescription.
   * See reference here: http://www.postgresql.org/docs/9.2/interactive/textsearch-controls.html
   *
   * @param query user inputted string
   * @return CelebrityListings of matching result.
   */
  def getCelebritiesBySearchAdmin(query: String) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val listings = celebrityStore.findByTextQuery(query)
      views.Application.admin.html.admin_celebrities_search(
        celebrityListings = listings
      )
    }
  }
}

object GetCelebritiesAdminEndpoint {

  def location: ActionDefinition = {
    WebsiteControllers.reverse(WebsiteControllers.getCelebritiesAdmin())
  }
}
