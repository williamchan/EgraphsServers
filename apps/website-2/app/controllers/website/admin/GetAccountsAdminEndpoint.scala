package controllers.website.admin

import models.AccountStore
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import services.http.AdminRequestFilters
import services.http.ControllerMethod

private[controllers] trait GetAccountsAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  def getAccountsAdmin(email: String = "") = Action { request =>
    controllerMethod() {
      adminFilters.requireAdministratorLogin { admin =>
        val account = if (!email.isEmpty) accountStore.findByEmail(email) else None
        Ok(views.html.Application.admin.admin_accounts(account = account))
      }
    }
  }
}

object GetAccountsAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getAccountsAdmin().url
//    WebsiteControllers.reverse(WebsiteControllers.getAccountsAdmin())
  }
}
