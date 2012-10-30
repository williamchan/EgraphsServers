package controllers.website.admin

import play.api.mvc.Controller
import models.filters._
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller, Request}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetFilterValueAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def filterStore: FilterStore
  protected def filterValueStore: FilterValueStore

  def getFilterValueAdmin(filterValueId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        filterValueStore.findById(filterValueId) match {
          case Some(filterValue) => {
             Ok(views.html.Application.admin.admin_filtervalue(filterValue=filterValue, errorFields = errorFields))
          }
          case _ => NotFound("No such filter value")
        }
      }
    }
  }
  
  def getCreateFilterValueAdmin(filterId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        filterStore.findById(filterId) match {
          case Some(filter) => { 
            val tempFilterValue = FilterValue(name=flash.get("name").getOrElse(""), publicName=flash.get("publicName").getOrElse(""), filterId=filterId)
            Ok(views.html.Application.admin.admin_filtervalue(filterValue = tempFilterValue, errorFields = errorFields))
          }
          case _ => NotFound("No such filter")
        }
      }
    }
  }
  
  private def fieldDefaults(implicit request: Request[play.api.mvc.AnyContent]) : (String => String) = {
	  (paramName: String) => paramName match {
	    case "name" => flash.get("name").getOrElse("")
	    case "publicName" => flash.get("publicName").getOrElse("")
	    case _ => flash.get(paramName).getOrElse("")
	  }
   }
  
  private def errorFields(implicit request: Request[play.api.mvc.AnyContent]) : Option[List[String]] = flash.get("errors").map(errString => errString.split(',').toList)

}

