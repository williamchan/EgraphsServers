package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{AccountStore, CelebrityStore}

private[controllers] trait GetUpdateAccountEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getUpdateAccount(accountId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

      val account = accountStore.findById(accountId).get
      val fieldDefaults: (String => String) = {
        (paramName: String) => paramName match {
          case "accountId" => accountId.toString
          case "email" => account.email
          case "password" => account.password.get.hash
          case _ =>
            Option(flash.get(paramName)).getOrElse("")
        }
      }
      views.Application.admin.html.admin_accountdetail(isCreate = false, errorFields = errorFields, fields = fieldDefaults)
    }
  }
}

object GetUpdateAccountEndpoint {

  def url(accountId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getUpdateAccount", Map("accountId" -> accountId.toString))
  }
}