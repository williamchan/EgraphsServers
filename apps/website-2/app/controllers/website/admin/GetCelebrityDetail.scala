package controllers.website.admin

import models.Celebrity
import models.enums.PublishedStatus
import org.apache.commons.lang.StringEscapeUtils
import play.api.mvc.Results.Ok

object GetCelebrityDetail {

  def getCelebrityDetail(isCreate: Boolean, celebrity: Option[Celebrity] = None
    )(implicit authToken: egraphs.authtoken.AuthenticityToken, flash: play.api.mvc.Flash): play.api.mvc.Result = {
    
    val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId").getOrElse("")
        case "celebrityEmail" => flash.get("celebrityEmail").getOrElse("")
        case "bio" => StringEscapeUtils.escapeHtml(flash.get("bio").getOrElse(""))
        case "casualName" => StringEscapeUtils.escapeHtml(flash.get("casualName").getOrElse(""))
        case "organization" => StringEscapeUtils.escapeHtml(flash.get("organization").getOrElse(""))
        case "publicName" => StringEscapeUtils.escapeHtml(flash.get("publicName").getOrElse(""))
        case "publishedStatusString" => flash.get("publishedStatusString").getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => StringEscapeUtils.escapeHtml(flash.get("roleDescription").getOrElse(""))
        case "twitterUsername" => StringEscapeUtils.escapeHtml(flash.get("twitterUsername").getOrElse(""))
        case _ => flash.get(paramName).getOrElse("")
      }
    }
    Ok(views.html.Application.admin.admin_celebritydetail(isCreate = isCreate, errorFields = errorFields, fields = fieldDefaults, celebrity = celebrity))
  }

}
