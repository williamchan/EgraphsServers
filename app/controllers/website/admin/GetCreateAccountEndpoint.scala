package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}

private[controllers] trait GetCreateAccountEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getCreateAccount() = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

      val fieldDefaults: (String => String) = {
        (paramName: String) => paramName match {
          case _ =>
            Option(flash.get(paramName)).getOrElse("")
        }
      }
      views.Application.admin.html.admin_accountdetail(isCreate = true, errorFields = errorFields, fields = fieldDefaults)
    }
  }
}

object GetCreateAccountEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getCreateAccount")
  }
}