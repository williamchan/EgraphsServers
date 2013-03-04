package controllers.website

import play.api._
import play.api.mvc._
import services.ConsumerApplication
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID
import models.frontend.login_page.{RegisterConsumerViewModel, LoginFormViewModel}
import services.http.forms.purchase.FormReaders
import services.mvc.forms.LoginFormViewConversions
import services.mvc.ImplicitHeaderAndFooterData
import controllers.WebsiteControllers
import egraphs.playutils.FlashableForm._
import controllers.website.consumer.PostRegisterConsumerEndpoint

private[controllers] trait GetLoginEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  import services.http.forms.Form.Conversions._

  //
  // Services
  //
  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def formReaders: FormReaders
  protected def consumerApp: ConsumerApplication

  //
  // Controllers
  //
  def getLogin(maybeBannerMessage: Option[String] = None) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      // Save a new FB state ID into the session
      val fbState = UUID.randomUUID().toString
      val fbCallbackUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getFacebookLoginCallback().url)
      val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState, fbCallbackUrl = fbCallbackUrl)
      implicit val flash = request.flash

      // Render
      Ok(views.html.frontend.login(
        loginForm = makeLoginFormView,
        registrationForm = PostRegisterConsumerEndpoint.form.bindWithFlashData,
        registrationActionUrl = controllers.routes.WebsiteControllers.postRegisterConsumerEndpoint.url,
        fbAuthUrl = fbOauthUrl,
        maybeBannerMessage = maybeBannerMessage
      )).withSession(request.session + (Facebook._fbState -> fbState))
    }
  }

  //
  // Private members
  //
  private def makeLoginFormView(implicit flash: Flash): LoginFormViewModel = {
    import LoginFormViewConversions._

    // Get form from flash if possible
    val flashAsReadable = flash.asFormReadable
    val maybeFormFromFlash = formReaders.forCustomerLoginForm.read(flashAsReadable)
    val maybeFormViewModel = maybeFormFromFlash.map(form => form.asView)

    // If we couldn't find the form in the flash we'll just make an empty form
    // with the right names
    maybeFormViewModel.getOrElse(LoginFormViewConversions.defaultView)
  }
}