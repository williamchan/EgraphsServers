package controllers.website.admin

import models.AccountStore
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.data._
import play.api.data.Forms._

private[controllers] trait GetAccountsAdminEndpoint {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  def getAccountsAdmin = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      Action { implicit request =>
        val email: String = Form("email" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        val account = if (!email.isEmpty) accountStore.findByEmail(email) else None
        Ok(views.html.Application.admin.admin_accounts(account = account))
      }
    }
  }
}

object GetAccountsAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getAccountsAdmin.url
  }
}
