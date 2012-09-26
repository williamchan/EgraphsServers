package controllers.website

import play.api._
import play.api.mvc._
import play.api.data.Forms.tuple
import play.api.data.Forms.text
import play.api.data.Forms.email
import play.api.data.Form
import services.http.ControllerMethod
import services.Utils
import models.{Account, AccountStore}
import models.frontend.forms.{FormError, Field}
import models.frontend.account.{AccountPasswordResetForm => AccountPasswordResetFormView}
import services.http.SafePlayParams.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountPasswordResetFormFactory
import services.http.forms.AccountPasswordResetForm.Fields
import services.http.filters.RequireAccountEmail
import services.http.filters.RequireResetPasswordSecret

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod() {
    httpFilters.requireAccountEmail.inFlashOrRequest() { account =>
      Action { implicit request =>
        val form = makeFormView(account)
  
        val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
          .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)
  
          if (account.verifyResetPasswordKey(form.secretKey.value.getOrElse("")) == true) {
            Ok(views.html.frontend.account_password_reset(form=form, displayableErrors=displayableErrors))
          } else {
            Forbidden("The password reset URL you used is either out of date or invalid.")
        }
      }
    }
  }

  def getVerifyAccount() = controllerMethod() {
    httpFilters.requireAccountEmail.inRequest() { account =>
      Action { request =>      
      val action = requireResetPasswordSecret(account) {
        Action {
          account.emailVerify().save()
          Ok(views.html.frontend.simple_confirmation("Account Verified", "Your account has been successfully verified."))
        }
      }

      action(request)
    }
  }

  private def makeFormView(account: Account)(implicit request: Request[_]) : AccountPasswordResetFormView = {
    //check flash for presence of secretKey and Email
    val flash = request.flash
    val maybeFormData = accountPasswordResetForms.getFormReader(account).read(flash.asFormReadable).map { form =>
      AccountPasswordResetFormView(
        form.secretKey.asViewField,
        form.email.asViewField,
        form.newPassword.asViewField,
        form.passwordConfirm.asViewField
      )
    }

    //check url params for secret key and email
    maybeFormData.getOrElse {
      val (emailString, secretKey) = Form(
        tuple(
          "email" -> email,
          "secretKey" -> text
        )
      ).bindFromRequest.fold(
        errors => ("", ""),
        emailAndSecret => emailAndSecret
      )
           
      val emails = Seq(emailString)
      val secretKeys = Seq(secretKey)

      AccountPasswordResetFormView(
        email = Field(name = Fields.Email.name, values = emails),
        secretKey = Field(name = Fields.SecretKey.name, values = secretKeys),
        passwordConfirm = Field(name = Fields.PasswordConfirm.name, values = List("")),
        newPassword = Field[String](name = Fields.NewPassword.name, values = List(""))
      )
    }
  }
}

object GetResetPasswordEndpoint {

  def absoluteUrl(email: String, secretKey: String): String = {
    val action = controllers.routes.WebsiteControllers.getResetPassword(email, secretKey).url
//    val action = Utils.lookupUrl("WebsiteControllers.getResetPassword",
//      Map("email" -> email, "secretKey" -> secretKey))
    Utils.absoluteUrl(action)
  }

  def redirectUrl : ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword")
  }
}
