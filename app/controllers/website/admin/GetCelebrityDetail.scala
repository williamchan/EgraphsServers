package controllers.website.admin

import play.mvc.Scope.Flash
import play.templates.Html

object GetCelebrityDetail {

  def getCelebrityDetail(isCreate: Boolean)(implicit flash: Flash): Html = {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId")
        case "celebrityEmail" => flash.get("celebrityEmail")
        case "firstName" => flash.get("firstName")
        case "lastName" => flash.get("lastName")
        case "publicName" => flash.get("publicName")
        case "description" => flash.get("description")
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }
    // Render the page
    views.Application.admin.html.admin_celebritydetail(isCreate = isCreate, errorFields = errorFields, fields = fieldDefaults)
  }

}
