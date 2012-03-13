package controllers.website.admin

import play.mvc.Controller
import services.Utils

private[controllers] trait GetCreateCelebrityEndpoint {
  this: Controller =>

  /**
   * Serves up the HTML for the Create Celebrity page.
   */
  def getCreateCelebrity = {
    // Get errors and param values from previous unsuccessful buy
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "isLeftHanded" => "true"
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }
    // Render the page
    views.Application.html.createcelebrity(errorFields = errorFields, fields = fieldDefaults)
  }
}

object GetCreateCelebrityEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getCreateCelebrity")
  }
}