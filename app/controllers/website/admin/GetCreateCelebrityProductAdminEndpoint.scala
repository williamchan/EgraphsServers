package controllers.website.admin

import play.mvc.Controller
import services.Utils
import models.Celebrity
import services.http.{ControllerMethod, AdminRequestFilters}

private[controllers] trait GetCreateCelebrityProductAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  /**
   * Serves up the HTML for the Create Celebrity page.
   */
  def getCreateCelebrityProductAdmin = controllerMethod() {
    adminFilters.requireCelebrity { (celebrity, admin) =>
      GetProductDetail.getCelebrityProductDetail(celebrity = celebrity, isCreate = true)
    }
  }
}

object GetCreateCelebrityProductAdminEndpoint {

  def url(celebrity: Celebrity) = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProductAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}