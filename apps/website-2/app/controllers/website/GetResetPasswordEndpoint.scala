package controllers.website

import play.api._
import play.api.mvc._
import services.http.ControllerMethod
import services.Utils
import models.{Account, AccountStore}
import models.frontend.forms.{FormError, Field}
import models.frontend.account.{AccountPasswordResetForm => AccountPasswordResetFormView}
import services.http.SafePlayParams.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountPasswordResetFormFactory
import services.http.forms.AccountPasswordResetForm.Fields
import services.http.filters.RequireValidAccountEmail

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def requireValidAccountEmail: RequireValidAccountEmail
  protected def accountStore: AccountStore
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod() {
    requireValidAccountEmail.inFlashOrRequest() { implicit request =>
      val form = makeFormView(request.account)

      val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
        .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)

        if (request.account.verifyResetPasswordKey(form.secretKey.value.getOrElse("")) == true) {
          Ok(views.html.frontend.account_password_reset(form=form, displayableErrors=displayableErrors))
        } else {
          Forbidden("The password reset URL you used is either out of date or invalid.")
      }
    }
  }

  def getVerifyAccount() = controllerMethod() { 
    Action { request =>
      val email = Utils.getFromMapFirstInSeqOrElse("email", "Nothing", request.queryString)
      accountRequestFilters.requireValidAccountEmail(email) { account =>
        val resetPasswordKey = Utils.getFromMapFirstInSeqOrElse("secretKey", "", request.queryString)
        if(account.verifyResetPasswordKey(resetPasswordKey)){
          account.emailVerify().save()
          Ok(views.html.frontend.simple_confirmation("Account Verified", "Your account has been successfully verified."))
        } else {
          Forbidden("The password reset URL you used is either out of date or invalid.")
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
      val params = request.queryString
      val emails = params.get("email").getOrElse(Seq(""))
      val secretKeys = params.get("secretKey").getOrElse(Seq(""))

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
