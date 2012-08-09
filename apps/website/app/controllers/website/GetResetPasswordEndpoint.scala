package controllers.website

import play.mvc.Controller
import services.http.{AccountRequestFilters, ControllerMethod}
import services.Utils
import models.{Account, AccountStore}
import models.frontend.forms.{FormError, Field}
import play.mvc.Router.ActionDefinition
import models.frontend.account.{AccountPasswordResetForm => AccountPasswordResetFormView}
import services.http.SafePlayParams.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountPasswordResetFormFactory
import services.http.forms.AccountPasswordResetForm.Fields

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def accountRequestFilters: AccountRequestFilters
  protected def accountPasswordResetForms: AccountPasswordResetFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod() {
    //flash takes precedence over url arg
    accountRequestFilters.requireValidAccountEmail(flash.getOption("email").getOrElse(email)) { account =>
      val form = makeFormView(account)

      val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
        .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)

        if (account.verifyResetPasswordKey(form.secretKey.value.getOrElse("")) == true) {
          views.frontend.html.account_password_reset(form=form, displayableErrors=displayableErrors)
        } else {
         Forbidden("The password reset URL you used is either out of date or invalid.")
      }
    }
  }

  def getVerifyAccount() =  controllerMethod() {
    accountRequestFilters.requireValidAccountEmail(request.params.getOption("email").getOrElse("Nothing")) { account =>
      if(account.verifyResetPasswordKey(request.params.getOption("secretKey").getOrElse(""))){
        account.emailVerify().save()
        views.frontend.html.simple_confirmation("Account Verified", "Your account has been successfully verified.")
      } else {
        Forbidden("The password reset URL you used is either out of date or invalid.")
      }
    }
  }

  private def makeFormView(account: Account) : AccountPasswordResetFormView = {
    //check flash for presence of secretKey and Email
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
      val emailOption = params.getOption("email")
      val secretKeyOption = params.getOption("secretKey")

      AccountPasswordResetFormView(
        email= Field(name = Fields.Email.name, values = List(emailOption.getOrElse(""))),
        secretKey = Field(name = Fields.SecretKey.name, values = List(secretKeyOption.getOrElse(""))),
        passwordConfirm = Field(name = Fields.PasswordConfirm.name, values = List("")),
        newPassword = Field[String](name = Fields.NewPassword.name, values = List(""))
      )
    }
  }
}

object GetResetPasswordEndpoint {

  def absoluteUrl(email: String, secretKey: String): String = {
    val action = Utils.lookupUrl("WebsiteControllers.getResetPassword",
      Map("email" -> email, "secretKey" -> secretKey))
    Utils.absoluteUrl(action)
  }

  def redirectUrl : ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword")
  }
}
