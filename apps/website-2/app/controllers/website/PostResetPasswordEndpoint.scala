package controllers.website

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.mvc.Results.{Forbidden, Redirect, Ok}
import models.AccountStore
import services.http.{SafePlayParams, POSTControllerMethod}
import services.http.forms.{AccountPasswordResetFormFactory, Form}
import services.mvc.ImplicitHeaderAndFooterData
import services.Utils
import services.http.filters.HttpFilters
import services.http.forms.AccountPasswordResetForm.Fields
import controllers.routes.WebsiteControllers.getResetPassword

private[controllers] trait PostResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import Form.Conversions._
  import SafePlayParams.Conversions._

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def postResetPassword() = postController() {
    httpFilters.requireAccountEmail.inRequest() { account =>
      Action { implicit request =>
        val params = request.queryString
        val nonValidatedForm = accountPasswordResetForms(request.asFormReadable, account)
  
        nonValidatedForm.errorsOrValidatedForm match {
          case Left(errors) =>
            // Try to get the bare minimum info to allow another submission of the form.
            //   otherwise it's unlikely that the form was submitted from us so we will
            //   forbid it.
            val minimalForm = play.api.data.Form(
              tuple(Fields.Email.name -> text, Fields.SecretKey.name -> text)
            )
            minimalForm.bindFromRequest.fold(
              errors => Forbidden,
              emailAndKey => {
                val (email, key) = emailAndKey
                nonValidatedForm.redirectThroughFlash(getResetPassword(email, key).url)
              }
            )

          // the form validates the secret key            
          case Right(validForm) => {
            val validationOrAccount = account.withPassword(validForm.passwordConfirm)
            for (validation <- validationOrAccount.left) yield {
              //Should never reach here, form validation should have caught any password problems.
              Forbidden("The reset url you are using is incorrect or expired.")
            }
            
            validationOrAccount.right.get.emailVerify().save()
            Ok(
              views.html.frontend.simple_confirmation(
                header = "Password Reset",
                body = "You have successfully changed your password!"
              )
            )
          }
        }
      }
    }
  }
}
