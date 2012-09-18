package controllers.website

import play.api._
import play.api.mvc._
import services.Utils
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID
import models.frontend.login_page.{AccountRegistrationFormViewModel, LoginFormViewModel}
import services.http.forms.purchase.FormReaders
import services.mvc.forms.{AccountRegistrationFormViewConversions, LoginFormViewConversions}
import services.mvc.ImplicitHeaderAndFooterData
import controllers.WebsiteControllers

private[controllers] trait GetLoginEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  import services.http.forms.Form.Conversions._

  //
  // Services
  //
  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def formReaders: FormReaders

  //
  // Controllers
  //
  def getLogin = Action { request =>
    controllerMethod() {
    
      // Save a new FB state ID into the session
      val fbState = UUID.randomUUID().toString
      val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState)
  
      // Render
      Ok(views.html.frontend.login(
        loginForm=makeLoginFormView,
        registrationForm=makeRegisterFormView,
        fbAuthUrl=fbOauthUrl
      )).withSession(request.session + (Facebook._fbState -> fbState))
    }
  }

  //
  // Private members
  //
  private def makeLoginFormView: LoginFormViewModel = {
    import LoginFormViewConversions._

    // Get form from flash if possible
    val flash = play.mvc.Http.Context.current().flash()
    val flashAsReadable = flash.asFormReadable
    val maybeFormFromFlash = formReaders.forCustomerLoginForm.read(flashAsReadable)
    val maybeFormViewModel = maybeFormFromFlash.map(form => form.asView)

    // If we couldn't find the form in the flash we'll just make an empty form
    // with the right names
    maybeFormViewModel.getOrElse(LoginFormViewConversions.defaultView)
  }

  private def makeRegisterFormView: AccountRegistrationFormViewModel = {
    import services.mvc.forms.AccountRegistrationFormViewConversions._

    // Get for form flash if possible
    val flash = play.mvc.Http.Context.current().flash()
    val flashAsReadable = flash.asFormReadable
    val maybeFormFromFlash = formReaders.forRegistrationForm.read(flashAsReadable)
    val maybeFormViewModel = maybeFormFromFlash.map(form => form.asView)

    // Get the default
    maybeFormViewModel.getOrElse(AccountRegistrationFormViewConversions.defaultView)
  }
}

object GetLoginEndpoint {
  def url() = {
    controllers.routes.WebsiteControllers.getLogin().url
//    WebsiteControllers.reverse(WebsiteControllers.getLogin)
  }
}
