package controllers.website.admin

import play.api.mvc.{Action,Controller}
import play.api.mvc.Results.Redirect
import services.http.{AdminRequestFilters, ControllerMethod}
import models.{Password, AccountStore, CelebrityStore}
import controllers.WebsiteControllers

private[controllers] trait GetAccountAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getAccountAdmin(accountId: Long) = Action { request =>
    controllerMethod() {
      adminFilters.requireAdministratorLogin({ admin =>
        val flash = play.mvc.Http.Context.current().flash()
        val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

        val account = accountStore.get(accountId)
        val password = account.password match {
          case Some(ps: Password) => ps.hash
          case None => ""
        }

        val fieldDefaults: (String => String) = {
          (paramName: String) =>
            paramName match {
              case "accountId" => accountId.toString
              case "email" => account.email
              case "password" => password
              case _ =>
                Option(flash.get(paramName)).getOrElse("")
            }
        }
        Ok(views.html.Application.admin.admin_accountdetail(isCreate = false, errorFields = errorFields, fields = fieldDefaults))
      })
    }
  }
}

object GetAccountAdminEndpoint {

  def url(accountId: Long) = {
    controllers.routes.WebsiteControllers.getAccountAdmin(accountId).url
//    WebsiteControllers.reverse(WebsiteControllers.getAccountAdmin(accountId))
  }
}