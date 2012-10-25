package controllers.website.admin

import play.api.mvc.Controller
import models.filters._
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetFilterAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def filterStore: FilterStore

  def getFilterAdmin(filterId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        filterStore.findById(filterId) match {
          case Some(filter) => {
            val errorFields = flash.get("errors").map(errString => errString.split(',').toList)
            val fieldDefaults: (String => String) = {
              (paramName: String) => paramName match {
                case "name" => flash.get("name").getOrElse("")
                case "publicName" => flash.get("publicname").getOrElse("")
                case _ => flash.get(paramName).getOrElse("")
              }
            }
             Ok(views.html.Application.admin.admin_filter(fields = fieldDefaults, filter=filter, errorFields = errorFields, filterValues = filter.filterValues))
          }
          case _ => NotFound("No such filter")
        }
      }
    }
  }
  
  def getCreateFilterAdmin() = controllerMethod.withForm() {
    implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val errorFields = flash.get("errors").map(errString => errString.split(',').toList)
        val fieldDefaults: (String => String) = {
          (paramName: String) => paramName match {
            case "name" => flash.get("name").getOrElse("")
            case "publicName" => flash.get("publicname").getOrElse("")
            case _ => flash.get(paramName).getOrElse("")
          }
        }
        val tempFilter = Filter(name=flash.get("name").getOrElse(""), publicName=flash.get("publicname").getOrElse(""))
        Ok(views.html.Application.admin.admin_filter(fields = fieldDefaults, filter = tempFilter, errorFields = errorFields, filterValues = List()))
      }
    }
  }
}

