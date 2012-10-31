package controllers.website.admin

import models.Celebrity
import models.enums.PublishedStatus
import org.apache.commons.lang3.StringEscapeUtils
import play.api.mvc.Results.Ok

object GetCelebrityDetail {

  def getCelebrityDetail(celebrity: Option[Celebrity] = None
    )(implicit authToken: egraphs.authtoken.AuthenticityToken,
               headerData: models.frontend.header.HeaderData,
               footerData: models.frontend.footer.FooterData, 
               flash: play.api.mvc.Flash): play.api.mvc.Result = {
    
    val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "celebrityId" => flash.get("celebrityId").getOrElse("")
        case "celebrityEmail" => flash.get("celebrityEmail").getOrElse("")
        case "bio" => StringEscapeUtils.escapeHtml4(flash.get("bio").getOrElse(""))
        case "casualName" => StringEscapeUtils.escapeHtml4(flash.get("casualName").getOrElse(""))
        case "organization" => StringEscapeUtils.escapeHtml4(flash.get("organization").getOrElse(""))
        case "publicName" => StringEscapeUtils.escapeHtml4(flash.get("publicName").getOrElse(""))
        case "publishedStatusString" => flash.get("publishedStatusString").getOrElse(PublishedStatus.Unpublished.toString)
        case "roleDescription" => StringEscapeUtils.escapeHtml4(flash.get("roleDescription").getOrElse(""))
        case "twitterUsername" => StringEscapeUtils.escapeHtml4(flash.get("twitterUsername").getOrElse(""))
        case _ => flash.get(paramName).getOrElse("")
      }
    }
    Ok(views.html.Application.admin.admin_celebritydetail(isCreate = celebrity.isEmpty, errorFields = errorFields, fields = fieldDefaults, celebrity = celebrity))
  }

}
