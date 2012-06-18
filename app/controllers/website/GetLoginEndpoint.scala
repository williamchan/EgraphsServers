package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import services.social.Facebook
import java.util.UUID
import services.http.forms.{CustomerLoginForm, CustomerLoginFormFactory}
import models.frontend.login_page.{LoginForm => LoginFormView}

private[controllers] trait GetLoginEndpoint { this: Controller =>
  import services.mvc.FormConversions._
  import services.http.forms.Form.Conversions._
  import models.frontend.login_page

  protected def facebookAppId: String
  protected def controllerMethod: ControllerMethod
  protected def customerLoginForms: CustomerLoginFormFactory

  def getLogin = controllerMethod() {
    // Save a new FB state ID into the session
    val fbState = UUID.randomUUID().toString
    session.put(Facebook._fbState, fbState)
    val fbOauthUrl = Facebook.getFbOauthUrl(fbAppId = facebookAppId, state = fbState)

    // Render
    views.Application.html.login(form=makeLoginFormView, fbOauthUrl=fbOauthUrl)
  }

  private def makeLoginFormView: LoginFormView = {
    // Get form from flash if possible
    val maybeFormData = customerLoginForms.read(flash.asFormReadable).map { form =>
      val nonFieldSpecificErrors = form.fieldInspecificErrors.map(error => error.asViewError)

      LoginFormView(
        form.email.asViewField, form.password.asViewField, nonFieldSpecificErrors
      )
    }

    // If we couldn't find the form in the flash. We'll just make an empty form
    // with the right names
    maybeFormData.getOrElse {
      import models.frontend.forms.{Field, FormError}
      import CustomerLoginForm.Fields

      login_page.LoginForm(
        Field(Fields.Email.name), Field(Fields.Password.name), List.empty[FormError]
      )
    }
  }
}

object GetLoginEndpoint {

  def url() = {
    Utils.lookupUrl("WebsiteControllers.getLogin")
  }
}