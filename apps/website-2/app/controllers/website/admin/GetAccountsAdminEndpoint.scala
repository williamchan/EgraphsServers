package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import play.mvc.Router.ActionDefinition
import models.AccountStore
import controllers.WebsiteControllers

private[controllers] trait GetAccountsAdminEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  def getAccountsAdmin(email: String = "") = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val account = if (!email.isEmpty) accountStore.findByEmail(email) else None
      views.Application.admin.html.admin_accounts(account = account)
    }
  }
}

object GetAccountsAdminEndpoint {

  def url(): ActionDefinition = {
    WebsiteControllers.reverse(WebsiteControllers.getAccountsAdmin())
  }
}
