package services.http.filters

import com.google.inject.Inject
import RequireAdministratorLogin.AdministratorAccount
import models.Account
import models.AccountStore
import models.Administrator
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import services.db.Saves
import services.http.EgraphsSession
import models.AdministratorStore

object RequireAdministratorLogin {
  type AdministratorAccount = (Administrator, Account)
}

class RequireAdministratorLogin @Inject() (adminStore: AdministratorStore, accountStore: AccountStore)
  extends Filter[Long, AdministratorAccount] with RequestFilter[Long, AdministratorAccount] {
  val redirectToLogin = Redirect(controllers.website.admin.GetLoginAdminEndpoint.url())

  override protected def badRequest(formWithErrors: Form[Long]): Result = redirectToLogin

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
