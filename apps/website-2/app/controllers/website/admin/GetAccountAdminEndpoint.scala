package controllers.website.admin

import play.api.mvc.{Action,Controller}
import play.api.mvc.Results.Redirect
import services.http.ControllerMethod
import models.{Password, AccountStore, CelebrityStore}
import controllers.WebsiteControllers
import services.http.filters.HttpFilters

private[controllers] trait GetAccountAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def celebrityStore: CelebrityStore

  def getAccountAdmin(accountId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      Action { implicit request =>
        val flash = request.flash
        val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

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
                flash.get(paramName).getOrElse("")
            }
        }
        
        Ok(views.html.Application.admin.admin_accountdetail(isCreate = false, errorFields = errorFields, fields = fieldDefaults))
      }
    }
  }
}

object GetAccountAdminEndpoint {

  def url(accountId: Long) = {
    controllers.routes.WebsiteControllers.getAccountAdmin(accountId).url
  }
}