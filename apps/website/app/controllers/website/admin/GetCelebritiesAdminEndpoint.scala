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

private[controllers] trait GetCelebritiesAdminEndpoint extends ImplicitHeaderAndFooterData  {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def controllerMethod: ControllerMethod

  def getCelebritiesAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        // get query parameters
        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)

        val query = celebrityStore.getCelebrityAccounts
        val pagedQuery: (Iterable[(Celebrity, Account)], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebritiesAdminEndpoint.location)
        Ok(views.html.Application.admin.admin_celebrities(
          celebrityAccounts = pagedQuery._1,
          celebrityStore.getAll // for the Featured Stars chooser
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
  
  def getRebuildSearchIndex = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        celebrityStore.rebuildSearchIndex
        Ok("Index has been rebuilt.")
      }
    }
    
  }
}

object GetCelebritiesAdminEndpoint {

  def location = {
    controllers.routes.WebsiteControllers.getCelebritiesAdmin.url
  }
}
