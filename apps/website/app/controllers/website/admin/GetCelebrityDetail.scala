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
        case "bio" => flash.get("bio")
        case "casualName" => flash.get("casualName")
        case "description" => flash.get("description")
        case "firstName" => flash.get("firstName")
        case "lastName" => flash.get("lastName")
        case "organization" => flash.get("organization")
        case "publicName" => flash.get("publicName")
        case "publishedStatusString" => Option(flash.get("publishedStatusString")).getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => flash.get("roleDescription")
        case "twitterUsername" => flash.get("twitterUsername")
        case _ => Option(flash.get(paramName)).getOrElse("")
      }
    }
    // Render the page
    views.Application.admin.html.admin_celebritydetail(isCreate = isCreate, errorFields = errorFields, fields = fieldDefaults, celebrity = celebrity)
  }

}
