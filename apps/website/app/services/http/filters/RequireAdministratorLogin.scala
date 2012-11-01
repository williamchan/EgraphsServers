package services.http.filters

import com.google.inject.Inject

import RequireAdministratorLogin.AdministratorAccount
import models.Account
import models.AccountStore
import models.Administrator
import models.AdministratorStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.Redirect
import play.api.mvc.Request
import play.api.mvc.Result
import services.http.EgraphsSession

object RequireAdministratorLogin {
  type AdministratorAccount = (Administrator, Account)
}

class RequireAdministratorLogin @Inject() (adminStore: AdministratorStore, accountStore: AccountStore)
  extends Filter[Long, AdministratorAccount] with RequestFilter[Long, AdministratorAccount] {
  val redirectToLogin = Redirect(controllers.website.admin.GetLoginAdminEndpoint.url())

  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    source match {
    case SessionSource => redirectToLogin.withSession(request.session - EgraphsSession.Key.AdminId.name)
    case _ => redirectToLogin
    }
  }

  override def filter(adminId: Long): Either[Result, AdministratorAccount] = {
    for (
      admin <- adminStore.findById(adminId).toRight(left = redirectToLogin).right;
      account <- accountStore.findByAdministratorId(adminId).toRight(left = redirectToLogin).right
    ) yield {
      (admin, account)
    }
  }

  override val form: Form[Long] = Form(
    single(
      EgraphsSession.Key.AdminId.name -> longNumber)
      verifying ("Invalid adminId", {
        case adminId => adminId > 0
      }: Long => Boolean))
}
