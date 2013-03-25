package controllers.website

import egraphs.playutils.FlashableForm._
import java.util.UUID
import play.api._
import play.api.mvc._
import services.ConsumerApplication
import services.http.ControllerMethod
import services.login.{PostLoginHelper, PostRegisterHelper}
import services.mvc.ImplicitHeaderAndFooterData
import services.social.Facebook

private[controllers] trait GetLoginEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  import services.http.forms.Form.Conversions._

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def consumerApp: ConsumerApplication

  def getLogin(maybeBannerMessage: Option[String] = None) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>

      // Save a new FB state ID into the session
      val fbState = UUID.randomUUID().toString
      val fbCallbackUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getFacebookLoginCallback().url)
      val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState, fbCallbackUrl = fbCallbackUrl)
      implicit val flash = request.flash

      Ok(views.html.frontend.login(
        loginForm = PostLoginHelper.form.bindWithFlashData(PostLoginHelper.formName),
        loginActionUrl = controllers.routes.WebsiteControllers.postLogin.url,
        forgotPasswordUrl = controllers.routes.WebsiteControllers.getRecoverAccount.url,
        registrationForm = PostRegisterHelper.form.bindWithFlashData(PostRegisterHelper.formName),
        registrationActionUrl = controllers.routes.WebsiteControllers.postRegisterConsumerEndpoint.url,
        fbAuthUrl = fbOauthUrl,
        maybeBannerMessage = maybeBannerMessage
      )).withSession(request.session + (Facebook._fbState -> fbState))
    }
  }
}