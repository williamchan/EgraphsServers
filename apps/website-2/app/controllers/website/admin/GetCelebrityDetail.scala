package controllers.website.admin

import play.templates.Html
import play.mvc.Scope.{Session, Flash}
import models.enums.PublishedStatus
import models.Celebrity
import org.apache.commons.lang.StringEscapeUtils

object GetCelebrityDetail {

  def getCelebrityDetail(isCreate: Boolean, celebrity: Option[Celebrity] = None)(implicit flash: Flash, session: Session): Html = {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId")
        case "celebrityEmail" => flash.get("celebrityEmail")
        case "bio" => StringEscapeUtils.escapeHtml(flash.get("bio"))
        case "casualName" => StringEscapeUtils.escapeHtml(flash.get("casualName"))
        case "organization" => StringEscapeUtils.escapeHtml(flash.get("organization"))
        case "publicName" => StringEscapeUtils.escapeHtml(flash.get("publicName"))
        case "publishedStatusString" => Option(flash.get("publishedStatusString")).getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => StringEscapeUtils.escapeHtml(flash.get("roleDescription"))
        case "twitterUsername" => StringEscapeUtils.escapeHtml(flash.get("twitterUsername"))
        case _ => Option(flash.get(paramName)).getOrElse("")
      }
    }
    // Render the page
    views.Application.admin.html.admin_celebritydetail(isCreate = isCreate, errorFields = errorFields, fields = fieldDefaults, celebrity = celebrity)
  }

}
