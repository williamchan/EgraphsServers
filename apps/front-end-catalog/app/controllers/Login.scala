package controllers

import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._
import models.frontend.login_page.{RegisterConsumerViewModel, LoginFormViewModel}
import models.frontend.forms.{FormError, Field}
import helpers.DefaultImplicitTemplateParameters

/**
 * Permutations of the Checkout: Login.
 */
object Login extends Controller with DefaultImplicitTemplateParameters {
  def storefront(recipient: Option[String]) = Action {
    Ok(LoginRenderArgs().copy(maybeGiftRecipient = recipient).renderCheckoutAsForm)
  }

  def index = Action {
    Ok(LoginRenderArgs().renderLoginForm)
  }

  def allErrorsLogin = Action {
    Ok(allErrorsLoginRenderArgs.renderLoginForm)
  }

  def allErrorsStorefront = Action {
    Ok(allErrorsLoginRenderArgs.renderCheckoutAsForm)
  }

  private def allErrorsLoginRenderArgs = {
    val defaultLogin = defaultLoginForm
    val defaultRegister = defaultRegistrationForm

    LoginRenderArgs(
      loginForm=defaultLogin.copy(
        defaultLogin.email.copy(values=Some("Erroneous email/user")).withError("Oops! Login email."),
        defaultLogin.password.copy(values=Some("Erroneous password")).withError("Oops! Login pass."),
        List(FormError("Oops! Login nebulous."))
      ),
      registrationForm=defaultRegister.bind(Map(
        ("email" -> "Erroneous register email"),
        ("password" -> "short"), // too short
        ("bulk-email" -> true.toString)
      ))
    )
  }

  private case class LoginRenderArgs(
    loginForm: LoginFormViewModel = defaultLoginForm,
    registrationForm: Form[RegisterConsumerViewModel] = defaultRegistrationForm,
    maybeGiftRecipient: Option[String] = None,
    celebrityName: String = "Herp Derpson",
    registerTargetUrl: String = "register-target-url",
    fbAuthUrl: String = "fbAuthUrl"
  ) {
    def renderLoginForm = {
      views.html.frontend.login(loginForm, registrationForm, registerTargetUrl, fbAuthUrl, None)
    }

    def renderCheckoutAsForm = {
      views.html.frontend.celebrity_storefront_login(
        loginForm,
        registerTargetUrl,
        celebrityName,
        maybeGiftRecipient,
        fbAuthUrl
      )
    }
  }

  private def defaultLoginForm = {
    LoginFormViewModel(
      Field("loginField", None),
      Field("passwordField", None),
      List.empty[FormError],
      "this-is-the-action-url",
      "this-is-the-forgot-password-url"
    )
  }

  private def defaultRegistrationForm = {
    // you could bind this to a Map[String, String] if you want to prepopulate
    simpleRegisterForm
  }

  private def simpleRegisterForm: Form[RegisterConsumerViewModel] = Form(mapping(
    "email" -> email.verifying(nonEmpty),
    "password" -> nonEmptyText(8),
    "bulk-email" -> boolean)(RegisterConsumerViewModel.apply)(RegisterConsumerViewModel.unapply)
  )

}

