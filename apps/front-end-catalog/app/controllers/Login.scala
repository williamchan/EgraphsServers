package controllers

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._
import models.frontend.login_page.{RegisterConsumerViewModel, LoginViewModel}
import models.frontend.forms.{FormError, Field}
import helpers.DefaultImplicitTemplateParameters

/**
 * Permutations of the Checkout: Login.
 */
object Login extends Controller with DefaultImplicitTemplateParameters {

  def index = Action {
    Ok(LoginRenderArgs().renderLoginForm)
  }

  def allErrorsLogin = Action {
    Ok(allErrorsLoginRenderArgs.renderLoginForm)
  }

  private def allErrorsLoginRenderArgs = {
    val defaultLogin = defaultLoginForm
    val defaultRegister = defaultRegistrationForm

    LoginRenderArgs(
      loginForm = defaultLogin.bind(Map(
        ("loginEmail" -> "Erroneous login email"),
        ("loginPassword" -> "Erroneous login password")
      )),
      registrationForm = defaultRegister.bind(Map(
        ("registerEmail" -> "Erroneous register email"),
        ("registerPassword" -> "short"), // too short
        ("bulk-email" -> true.toString)
      ))
    )
  }

  private case class LoginRenderArgs(
    loginForm: Form[LoginViewModel] = defaultLoginForm,
    loginActionUrl: String = "login-action-url",
    forgotPasswordUrl: String = "forgot-password-url",
    registrationForm: Form[RegisterConsumerViewModel] = defaultRegistrationForm,
    registrationActionUrl: String = "register-action-url",
    fbAuthUrl: String = "fbAuthUrl",
    maybeBannerMessage: Option[String] = None
  ) {
    def renderLoginForm = {
      views.html.frontend.login(
        loginForm,
        loginActionUrl,
        forgotPasswordUrl,
        registrationForm,
        registrationActionUrl,
        fbAuthUrl,
        maybeBannerMessage)
    }
  }

  private def defaultLoginForm = {
    simpleLoginForm
  }

  private def simpleLoginForm: Form[LoginViewModel] = Form(mapping(
    "loginEmail" -> email.verifying(nonEmpty),
    "loginPassword" -> nonEmptyText)
    (LoginViewModel.apply)(LoginViewModel.unapply)
  )

  private def defaultRegistrationForm = {
    // you could bind this to a Map[String, String] if you want to prepopulate
    simpleRegisterForm
  }

  private def simpleRegisterForm: Form[RegisterConsumerViewModel] = Form(mapping(
    "registerEmail" -> email.verifying(nonEmpty),
    "registerPassword" -> nonEmptyText(8),
    "bulk-email" -> boolean)
    (RegisterConsumerViewModel.apply)(RegisterConsumerViewModel.unapply)
  )

}

