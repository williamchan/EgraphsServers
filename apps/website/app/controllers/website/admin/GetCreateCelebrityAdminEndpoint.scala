package controllers.website.admin

import play.api.mvc.Controller
import models.{AccountStore, CelebrityStore}
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCreateCelebrityAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCreateCelebrityAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        implicit val flash = request.flash
        GetCelebrityDetail.getCelebrityDetail()
      }
    }
  }
}

object GetCreateCelebrityAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getCreateCelebrityAdmin.url
  }
}