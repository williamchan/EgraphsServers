package controllers.website.admin

import play.api.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}
import controllers.WebsiteControllers

private[controllers] trait GetCreateAccountAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCreateAccountAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

      val fieldDefaults: (String => String) = {
        (paramName: String) => paramName match {
          case _ =>
            Option(flash.get(paramName)).getOrElse("")
        }
      }
      views.html.Application.admin.admin_accountdetail(isCreate = true, errorFields = errorFields, fields = fieldDefaults)
    }
  }
}

object GetCreateAccountAdminEndpoint {

  def url() = {
    WebsiteControllers.reverse(WebsiteControllers.getCreateAccountAdmin)
  }
}