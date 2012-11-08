package controllers.website.admin

import models.AccountStore
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.data._
import play.api.data.Forms._
import services.mvc.{celebrity, ImplicitHeaderAndFooterData}

private[controllers] trait GetAccountsAdminEndpoint extends ImplicitHeaderAndFooterData  {
  this: Controller =>

  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def controllerMethod: ControllerMethod

  def getAccountsAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
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
