package controllers.website.admin

import play.api.mvc.Controller
import models.Celebrity
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.mvc.ImplicitHeaderAndFooterData
import org.apache.commons.lang3.StringEscapeUtils

private[controllers] trait GetCreateFreegraphAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCreateFreegraphAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val errorFields = flash.get("errors").map(errString => errString.split(',').toList)
	    val fieldDefaults: (String => String) = {
	      (paramName: String) => paramName match {
	        case _ => flash.get(paramName).getOrElse("")
	      }
	    }
	
	    Ok(views.html.Application.admin.admin_create_freegraph(
	      errorFields = errorFields,
	      fields = fieldDefaults))
      }
    }
  }
}

object GetCreateFreegraphAdminEndpoint {

  def url = {
    controllers.routes.WebsiteControllers.getCreateFreegraphAdmin.url
  }
}