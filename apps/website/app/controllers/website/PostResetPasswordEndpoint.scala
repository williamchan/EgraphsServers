package controllers.website

import play.mvc.Controller
import models.AccountStore
import services.http.{SafePlayParams, AccountRequestFilters, POSTControllerMethod}
import models.frontend.account.{AccountVerificationForm => AccountVerificationFormView}
import services.http.forms.{AccountVerificationForm, AccountVerificationFormFactory, Form}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait PostResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import Form.Conversions._
  import SafePlayParams.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def accountVerificationForms: AccountVerificationFormFactory
  protected def accountRequestFilters: AccountRequestFilters

  def postResetPassword(email: String) = postController() {

    accountRequestFilters.requireValidAccountEmail(email) { account =>
      val nonValidatedForm = accountVerificationForms(params.asFormReadable, account)
        nonValidatedForm.errorsOrValidatedForm match {
          case Left(errors) => {
            nonValidatedForm.redirectThroughFlash(GetResetPasswordEndpoint.redirectUrl.url)
          }
          //form validates secret key
          case Right(validForm) => {
              val validationOrAccount = account.withPassword(validForm.passwordConfirm)
              for (validation <- validationOrAccount.left) yield {
                //Should never reach here, form validation should have caught any password problems.
                Forbidden("The reset url you are using is incorrect or expired.")
              }
              validationOrAccount.right.get.save()
              views.frontend.html.simple_confirmation(header = "Password Reset", body ="Change this")
            }
          }
      }
    }
}
