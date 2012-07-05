package controllers.website

import play.mvc.Controller
import services.http.{AccountRequestFilters, ControllerMethod}
import services.Utils
import models.{Account, AccountStore}
import models.frontend.forms.{FormError, Field}
import play.mvc.Router.ActionDefinition
import models.frontend.account.{AccountVerificationForm => AccountVerificationFormView}
import services.http.SafePlayParams.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountVerificationFormFactory
import services.http.forms.AccountVerificationForm.Fields

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def accountRequestFilters: AccountRequestFilters
  protected def accountVerificationForms: AccountVerificationFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod() {
    //flash takes precedence over url arg
    accountRequestFilters.requireValidAccountEmail(flash.getOption("email").getOrElse(email)) { account =>
      val form = makeFormView(account)

      val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
        .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)

        if (account.verifyResetPasswordKey(form.secretKey.value.getOrElse("")) == true) {
          views.frontend.html.account_verification(form=form, displayableErrors=displayableErrors)
        } else {
         Forbidden("The password reset URL you used is either out of date or invalid.")
      }
    }
  }

  private def makeFormView(account: Account) : AccountVerificationFormView = {
    //check flash for presence of secretKey and Email
    val maybeFormData = accountVerificationForms.getFormReader(account).read(flash.asFormReadable).map { form =>
      AccountVerificationFormView(
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

      AccountVerificationFormView(
        email= Field(name = Fields.Email.name, values = List(emailOption.getOrElse(""))),
        secretKey = Field(name = Fields.SecretKey.name, values = List(secretKeyOption.getOrElse(""))),
        passwordConfirm = Field(name = Fields.PasswordConfirm.name, values = List("")),
        newPassword = Field[String](name = Fields.NewPassword.name, values = List(""))
      )
    }
  }
}

object GetResetPasswordEndpoint {

  def absoluteUrl(email: String, secretKey: String): ActionDefinition = {
    Utils.lookupAbsoluteUrl("WebsiteControllers.getResetPassword",
      Map("email" -> email, "secretKey" -> secretKey))
  }

  def redirectUrl : ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword")
  }
}
