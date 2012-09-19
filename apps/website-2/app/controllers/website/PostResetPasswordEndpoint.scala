package controllers.website

import play.api._
import play.api.mvc._
import models.AccountStore
import services.http.{SafePlayParams, AccountRequestFilters, POSTControllerMethod}
import services.http.forms.{AccountPasswordResetFormFactory, Form}
import services.mvc.ImplicitHeaderAndFooterData
import services.Utils

private[controllers] trait PostResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import Form.Conversions._
  import SafePlayParams.Conversions._

  protected def postController: POSTControllerMethod
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory
  protected def accountRequestFilters: AccountRequestFilters

  def postResetPassword() = Action { implicit request =>
    postController() {
      val email = Utils.getFromMapFirstInSeqOrElse("email", "Nothing", request.queryString)
      accountRequestFilters.requireValidAccountEmail(email) { account =>
        val params = request.queryString
        val nonValidatedForm = accountPasswordResetForms(params.asFormReadable, account)
  
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
            validationOrAccount.right.get.emailVerify().save()
            views.html.frontend.simple_confirmation(
              header = "Password Reset",
              body = "You have successfully changed your password!"
            )
          }
        }
      }
    }
  }
}
