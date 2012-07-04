package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import models.AccountStore
import services.http.POSTControllerMethod
import models.frontend.account.{AccountVerificationForm => AccountVerificationFormView}
import services.http.forms.{AccountVerificationForm, AccountVerificationFormFactory, Form}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait PostResetPasswordEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>
  import Form.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def accountVerificationForms: AccountVerificationFormFactory


  def postResetPassword() = postController() {
    val nonValidatedForm = accountVerificationForms(params.asFormReadable)

    nonValidatedForm.errorsOrValidatedForm match {
      case Left(errors) => {
        nonValidatedForm.redirectThroughFlash(GetResetPasswordEndpoint.redirectUrl.url)
      }
      case Right(validForm) => {
        for(account <- accountStore.findByEmail(validForm.email); if account.verifyResetPasswordKey(validForm.secretKey))
          yield {views.frontend.html.simple_confirmation(header = "Password Reset", body ="Change this")}
      }
    }

    Forbidden("The reset url you are using is incorrect or expired.")
  }
}
