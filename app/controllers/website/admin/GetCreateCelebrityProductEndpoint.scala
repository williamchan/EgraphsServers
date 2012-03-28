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
    // Get errors and param values from previous unsuccessful buy
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
      val fieldDefaults: (String => String) = {
        (paramName: String) => paramName match {
          case _ =>
            Option(flash.get(paramName)).getOrElse("")
        }
      }

      // Render the page
      views.Application.html.createcelebrityproduct(celebrity = celebrity, errorFields = errorFields, fields = fieldDefaults)
    }
  }
}

object GetCreateCelebrityProductEndpoint {

  def url(celebrity: Celebrity) = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProduct", Map("celebrityId" -> celebrity.id.toString))
  }
}