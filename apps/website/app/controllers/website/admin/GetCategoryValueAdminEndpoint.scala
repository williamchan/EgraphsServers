package controllers.website.admin

import play.api.mvc.Controller
import models.categories._
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller, Request}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCategoryValueAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def categoryStore: CategoryStore
  protected def categoryValueStore: CategoryValueStore

  def getCategoryValueAdmin(categoryValueId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        categoryValueStore.findById(categoryValueId) match {
          case Some(categoryValue) => {

             val currentCategoryIds =  (for(category <- categoryValue.categories) yield { category.id }).toSet
             Ok(views.html.Application.admin.admin_categoryvalue(categoryValue=categoryValue, errorFields = errorFields, categories = categoryStore.getCategories, currentCategoryIds=currentCategoryIds))
          }
          case _ => NotFound("No such category value")
        }
      }
    }
  }
  
  def getCreateCategoryValueAdmin(categoryId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        categoryStore.findById(categoryId) match {
          case Some(category) => { 
            val tempCategoryValue = CategoryValue(name=flash.get("name").getOrElse(""), publicName=flash.get("publicName").getOrElse(""), categoryId=categoryId)
            Ok(views.html.Application.admin.admin_categoryvalue(categoryValue = tempCategoryValue, errorFields = errorFields, categories = categoryStore.getCategories, currentCategoryIds=Set[Long]()))
          }
          case _ => NotFound("No such category")
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

