package controllers.website.admin

import play.api.mvc.Controller
import models.Celebrity
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCreateCelebrityProductAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCreateCelebrityProductAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { implicit celebrity =>
        Action { implicit request =>
          implicit val flash = request.flash + ("signingOriginX" -> "0")
          GetProductDetail.getCelebrityProductDetail(celebrity = celebrity)
        }
      }
    }
  }
}

object GetCreateCelebrityProductAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCreateCelebrityProductAdmin(celebrityId = celebrity.id).url
  }
}