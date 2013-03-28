package controllers.website

import egraphs.authtoken.AuthenticityToken
import egraphs.playutils.FlashableForm._
import models.AccountStore
import models.frontend.website.RecoverAccountViewModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.Valid
import play.api.mvc._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import services.email.ResetPasswordEmail
import services.http.filters.HttpFilters
import services.http.forms.FormConstraints
import services.http.POSTControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.Utils

private[controllers] trait PostRecoverAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def accountStore: AccountStore
  protected def dbSession: DBSession
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters  

  def postRecoverAccount() = postController() {
    AuthenticityToken.makeAvailable() { implicit authToken =>
      Action { implicit request =>

        val (form, formName) = (PostRecoverAccountEndpoint.form, PostRecoverAccountEndpoint.formName)

        form.bindFromRequest.fold(
          formWithErrors => Redirect(controllers.routes.WebsiteControllers.getRecoverAccount()).flashingFormData(formWithErrors, formName),
          validForm => {

            val email = validForm.email
            val customerAccount = accountStore.findByEmail(email).getOrElse(
              throw new RuntimeException("The email provided by existing user " +
              email + " somehow passed validation but failed while attempting to retrieve the account"))

            val accountWithResetPassKey = dbSession.connected(TransactionSerializable) {
              customerAccount.withResetPasswordKey.save()
            }

            // Send out reset password email
            ResetPasswordEmail(account = accountWithResetPassKey).send()

            Redirect(controllers.routes.WebsiteControllers.getSimpleMessage(
              header = "Success",
              body = "Instructions for recovering your account have been sent to your email address."
            ))
          }
        )
      }
    }
  }
}

object PostRecoverAccountEndpoint {
  def formName = "recover-account-form"
  def formConstraints = AppConfig.instance[FormConstraints]

  def form: Form[RecoverAccountViewModel] = Form(
    mapping(
      "email" -> email.verifying(nonEmpty, formConstraints.isValidCustomerEmail)
    )(RecoverAccountViewModel.apply)(RecoverAccountViewModel.unapply)
  )
}