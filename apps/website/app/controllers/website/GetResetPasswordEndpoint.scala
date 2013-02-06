package controllers.website

import play.api.mvc._
import play.api.data.Forms.{tuple, text, optional}
import play.api.data.Form
import services.http.ControllerMethod
import models.{Account, AccountStore}
import models.frontend.forms.{FormError, Field}
import models.frontend.account.{AccountPasswordResetForm => AccountPasswordResetFormView}
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountPasswordResetFormFactory
import services.http.forms.AccountPasswordResetForm.Fields
import services.http.filters.HttpFilters

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod.withForm()
  { implicit authToken =>
    httpFilters.requireAccountEmail.inFlashOrRequest() { account =>
      Action { implicit request =>
        val form = makeFormView(account)

        val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
          .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)

        if (account.verifyResetPasswordKey(secretKey) == true) {
          Ok(views.html.frontend.account_password_reset(form = form, displayableErrors = displayableErrors))
        } else {
          Forbidden(views.html.frontend.simple_message("Account not verified",
            "<p>Aww shucks! The password reset URL you used is invalid.</p>" +
              "<p>Sorry for this inconvenience, but it's for your own security.</p>" +
              "<p>Can you make sure that you're using the latest password reset email you received from us?</p>"
          ))
        }
      }
    }
  }

  def getVerifyAccount(email: String, resetKey: String) = controllerMethod.withForm()
  { implicit authToken =>
    httpFilters.requireAccountEmail(email) { account =>
      Action { implicit request =>
        if (account.verifyResetPasswordKey(resetKey)) {
          account.emailVerify().save()
          Ok(views.html.frontend.simple_message("Account Verified", "Your account has been successfully verified."))
        } else {
          Forbidden(views.html.frontend.simple_message("Account not verified",
            "Oh darn! The password reset URL you used is either out of date or invalid."))
        }
      }
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
      val (maybeEmailString, maybeSecretKey) = Form(
        tuple(
          "email" -> optional(text),
          "secretKey" -> optional(text)
        )
      ).bindFromRequest.fold(
        errors => (None, None),
        emailAndSecret => emailAndSecret
      )

      AccountPasswordResetFormView(
        email = Field(name = Fields.Email.name, values = maybeEmailString),
        secretKey = Field(name = Fields.SecretKey.name, values = maybeSecretKey),
        passwordConfirm = Field(name = Fields.PasswordConfirm.name, values = List("")),
        newPassword = Field[String](name = Fields.NewPassword.name, values = List(""))
      )
    }
  }
}
