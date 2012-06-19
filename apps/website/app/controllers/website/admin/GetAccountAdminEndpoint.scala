package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{Password, AccountStore, CelebrityStore}

private[controllers] trait GetAccountAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getAccountAdmin(accountId: Long) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

      val account = accountStore.findById(accountId).get
      val password = account.password match {
        case Some(ps: Password) => ps.hash
        case None => ""
      }

      val fieldDefaults: (String => String) = {
        (paramName: String) => paramName match {
          case "accountId" => accountId.toString
          case "email" => account.email
          case "password" => password
          case _ =>
            Option(flash.get(paramName)).getOrElse("")
        }
      }
      views.Application.admin.html.admin_accountdetail(isCreate = false, errorFields = errorFields, fields = fieldDefaults)
    }
  }
}

object GetAccountAdminEndpoint {

  def url(accountId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getAccountAdmin", Map("accountId" -> accountId.toString))
  }
}