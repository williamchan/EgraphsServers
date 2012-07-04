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

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>

  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def accountRequestFilters: AccountRequestFilters
  protected def accountVerificationForms: AccountVerificationFormFactory

  def getResetPassword(email: String, secretKey: String) = controllerMethod() {
    accountRequestFilters.requireValidAccountEmail(email) { account =>
      val form = makeFormView(account)

      val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error, form.email.error)
        .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)
      //check for email
//      println("Secret Key is: " + form.secretKey.value.getOrElse("no value"))
//      println("Secret Key variable: " + secretKey)
//      for(secretKey <- form.secretKey.value) yield {
        if (account.verifyResetPasswordKey(form.secretKey.name) == true) {
          views.frontend.html.account_verification(form=form, displayableErrors=displayableErrors)
//        }
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
        email = Field[String](emailOption.getOrElse("")),
        secretKey = Field[String](secretKeyOption.getOrElse("")),
        passwordConfirm = Field[String](""),
        newPassword = Field[String]("")
      )
    }
  }
}

object GetResetPasswordEndpoint {

  def url(accountId: Long, resetPasswordKey: String): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword",
      Map("accountId" -> accountId.toString, "passwordRecoveryKey" -> resetPasswordKey))
  }

  def redirectUrl : ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword")
  }
}
