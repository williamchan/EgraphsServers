package controllers.website

import controllers.routes.WebsiteControllers.{getRecoverAccount, getSimpleMessage}
import egraphs.authtoken.AuthenticityToken
import egraphs.playutils.FlashableForm._
import models.AccountStore
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import play.api.mvc._
import play.api.mvc.Results._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import services.email.ResetPasswordEmail
import services.http.filters.HttpFilters
import services.http.forms.FormConstraints
import services.http.POSTControllerMethod
import services.logging.Logging
import services.mvc.ImplicitHeaderAndFooterData
import services.Utils

private[controllers] trait PostRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  import PostRecoverAccountEndpoint._
  protected def accountStore: AccountStore
  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters

  def postRecoverAccount() = postController() {

    AuthenticityToken.makeAvailable() { implicit authToken =>
      Action { implicit request =>
        val boundForm = form.bindFromRequest
        boundForm.fold(
          formWithErrors => Redirect(failureTarget).flashingFormData(formWithErrors, formName),
          validEmail => {
            accountStore.findByEmail(validEmail) map { customerAccount =>
              val accountWithResetPassKey = dbSession.connected(TransactionSerializable) {
                customerAccount.withResetPasswordKey.save()
              }

              // Send out reset password email
              ResetPasswordEmail(account = accountWithResetPassKey).send()

              Redirect(successTarget)

            } getOrElse {
              log(s"Failed to reset password for ${validEmail}; passed form validation but failed to retrieve account")
              Redirect(failureTarget).flashingFormData(boundForm, formName)
            }
          }
        )
      }
    }
  }

}

object PostRecoverAccountEndpoint extends Logging {
  object keys {
    def email = "email"
  }

  def formName = "recover-account-form"
  def formConstraints = AppConfig.instance[FormConstraints]
  def form = Form(
    single(keys.email -> email.verifying(formConstraints.isValidCustomerEmail))
  )

  protected[website] def failureTarget = getRecoverAccount()
  protected[website] def successTarget = getSimpleMessage(
    header = "Success",
    body = "Instructions for recovering your account have been sent to your email address."
  )
}