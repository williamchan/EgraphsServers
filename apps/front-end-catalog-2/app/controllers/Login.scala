package controllers

import play.mvc.Controller
import models.frontend.login_page.{AccountRegistrationFormViewModel, LoginFormViewModel}
import models.frontend.forms.{FormError, Field}


/**
 * Permutations of the Checkout: Login.
 */
object Login extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  def storefront(recipient: Option[String]) = {
    LoginRenderArgs().copy(maybeGiftRecipient = recipient).renderCheckoutAsForm()
  }

  def index = {
    LoginRenderArgs().renderLoginForm()
  }

  def allErrorsLogin = {
    allErrorsLoginRenderArgs.renderLoginForm()
  }

  def allErrorsStorefront = {
    allErrorsLoginRenderArgs.renderCheckoutAsForm()
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
      registrationForm=defaultRegister.copy(
        defaultRegister.email.copy(values=Some("Erroneous register email")).withError("Oops! Register email."),
        defaultRegister.password.copy(values=Some("Erroneous password email")).withError("Oops! Register pass."),
        List(FormError("Oops! Register nebulous."))
      )
    )
  }

  private case class LoginRenderArgs(
    loginForm: LoginFormViewModel = defaultLoginForm,
    registrationForm: AccountRegistrationFormViewModel = defaultRegistrationForm,
    maybeGiftRecipient: Option[String] = None,
    celebrityName: String = "Herp Derpson",
    newOwnerTargetUrl: String = "new-owner-target-url",
    fbAuthUrl: String = "fbAuthUrl"
  ) {
    def renderLoginForm() = {
      views.frontend.html.login(loginForm, registrationForm, fbAuthUrl)
    }

    def renderCheckoutAsForm() = {
      views.frontend.html.celebrity_storefront_login(
        loginForm,
        newOwnerTargetUrl,
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
    AccountRegistrationFormViewModel(
      Field("emailField", None),
      Field("passwordField", None),
      List.empty[FormError],
      "this-is-the-action-url"
    )
  }


}

