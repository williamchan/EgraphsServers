package controllers.website

import play.api.data.Forms._
import play.api.mvc._
import play.api.mvc.Results._
import models.AccountStore
import services.http.POSTControllerMethod
import services.http.forms.{AccountPasswordResetFormFactory, Form}
import services.mvc.ImplicitHeaderAndFooterData
import services.http.filters.HttpFilters
import services.http.forms.AccountPasswordResetForm.Fields
import controllers.routes.WebsiteControllers.getResetPassword
import egraphs.authtoken.AuthenticityToken
import Form.Conversions._

private[controllers] trait PostResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  import PostResetPasswordEndpoint._

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def postResetPassword() = postController() {
    AuthenticityToken.makeAvailable() { implicit authToken =>
      httpFilters.requireAccountEmail.inRequest() { account =>
        Action { implicit request =>
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
              Redirect(successTarget)
            }
          }
        }
      }
    }
  }
}

object PostResetPasswordEndpoint {
  protected[website] def successTarget = controllers.routes.WebsiteControllers.getSimpleMessage(
    header = "Password Reset",
    body = "You have successfully changed your password!"
  )
}
