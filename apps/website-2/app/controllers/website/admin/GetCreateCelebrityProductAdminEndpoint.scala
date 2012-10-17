package controllers.website.admin

import play.api.mvc.Controller
import models.Celebrity
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}

private[controllers] trait GetCreateCelebrityProductAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCreateCelebrityProductAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      httpFilters.requireCelebrityId(celebrityId) { implicit celebrity =>
        Action { implicit request =>
          implicit val flash = request.flash
          GetProductDetail.getCelebrityProductDetail(celebrity = celebrity, isCreate = true)
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