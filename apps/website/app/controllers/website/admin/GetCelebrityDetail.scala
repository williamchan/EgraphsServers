package controllers.website.admin

import play.templates.Html
import play.mvc.Scope.{Session, Flash}
import models.enums.PublishedStatus
import models.Celebrity

object GetCelebrityDetail {

  def getCelebrityDetail(isCreate: Boolean, celebrity: Option[Celebrity] = None)(implicit flash: Flash, session: Session): Html = {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId")
        case "celebrityEmail" => flash.get("celebrityEmail")
        case "firstName" => flash.get("firstName")
        case "lastName" => flash.get("lastName")
        case "publicName" => flash.get("publicName")
        case "description" => flash.get("description")
        case "publishedStatusString" =>
          Option(flash.get("publishedStatusString")).getOrElse(PublishedStatus.Unpublished.toString)
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }
    // Render the page
    views.Application.admin.html.admin_celebritydetail(isCreate = isCreate, errorFields = errorFields, fields = fieldDefaults, celebrity = celebrity)
  }

}
