package controllers.website.admin

import play.api.mvc.Controller
import models.categories._
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller, Request}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCategoryAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def categoryStore: CategoryStore

  def getCategoryAdmin(categoryId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        categoryStore.findById(categoryId) match {
          case Some(category) => {
             Ok(views.html.Application.admin.admin_category(category=category, errorFields = errorFields, categoryValues = category.categoryValues))
          }
          case _ => NotFound("No such category")
        }
      }
    }
  }
  
  def getCreateCategoryAdmin() = controllerMethod.withForm() {
    implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        // Temporary category that does not get saved to the database. Used to store field values and render the template. 
        val tempCategory = Category(name=flash.get("name").getOrElse(""), publicName=flash.get("publicName").getOrElse(""))
        Ok(views.html.Application.admin.admin_category(category = tempCategory, errorFields = errorFields, categoryValues = List()))
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

