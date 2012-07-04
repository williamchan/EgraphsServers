package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import models.AccountStore
import play.mvc.Router.ActionDefinition
import models.frontend.account.{AccountVerificationForm => AccountVerificationFormView}
import services.http.SafePlayParams.Conversions._
import services.mvc.ImplicitHeaderAndFooterData
import services.http.forms.AccountVerificationFormFactory
import models.frontend.forms.{FormError, Field}

private[controllers] trait GetResetPasswordEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def accountVerificationForms: AccountVerificationFormFactory

  def getResetPassword() = controllerMethod() {
    val form = makeFormView()

    val displayableErrors = List(form.newPassword.error, form.passwordConfirm.error)
      .asInstanceOf[List[Option[FormError]]].filter(e => e.isDefined).map(e => e.get.description)

    //check for email
    accountStore.findByEmail(form.email.value.get) match {
      case Some(account) if account.verifyResetPasswordKey(form.secretKey.value.get) => {
        views.frontend.html.account_verification(form=form, displayableErrors=displayableErrors)
      }
      case _ =>
        Forbidden("The password reset URL you used is either out of date or invalid.")
    }
  }

  private def makeFormView() : AccountVerificationFormView = {
    //check flash for presence of secretKey and Email
    val maybeFormData = accountVerificationForms.read(flash.asFormReadable).map { form =>
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
