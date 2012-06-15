package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID
import services.http.forms.{CustomerLoginFormFactory}

private[controllers] trait GetLoginEndpoint { this: Controller =>
  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def customerLoginForms: CustomerLoginFormFactory

  def getLogin = controllerMethod() {
    val form = customerLoginForms(flash.asFormReadable)

    val generalErrors = if (form.wasRead) {
      for (error <- form.derivedErrors) yield error.asViewError
    } else {
      List()
    }

    val fbState = UUID.randomUUID().toString
    session.put(Facebook._fbState, fbState)
    val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState)

    views.Application.html.login(
      email=form.email.asViewField(withErrors=form.wasRead),
      password=form.password.asViewField(withErrors=form.wasRead),
      generalErrors=generalErrors,
      fbOauthUrl=fbOauthUrl
    )
  }
}

object GetLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLogin")
  }
}