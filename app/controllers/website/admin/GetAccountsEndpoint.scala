package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.Utils
import play.mvc.Router.ActionDefinition
import models.AccountStore

private[controllers] trait GetAccountsEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  def getAccounts(email: String = "") = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      var account = if (!email.isEmpty) accountStore.findByEmail(email) else None
      views.Application.admin.html.admin_accounts(account = account)
    }
  }
}

object GetAccountsEndpoint {

  def url(): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getAccounts")
  }
}
