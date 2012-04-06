package controllers.website.admin

import play.mvc.Controller
import services.Utils
import models.Celebrity
import services.http.{ControllerMethod, AdminRequestFilters}

private[controllers] trait GetCreateCelebrityProductEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  /**
   * Serves up the HTML for the Create Celebrity page.
   */
  def getCreateCelebrityProduct = controllerMethod() {
    adminFilters.requireCelebrity { (celebrity) =>
      GetProductDetail.getCelebrityProductDetail(celebrity = celebrity, isCreate = true)
    }
  }
}

object GetCreateCelebrityProductEndpoint {

  def url(celebrity: Celebrity) = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProduct", Map("celebrityId" -> celebrity.id.toString))
  }
}