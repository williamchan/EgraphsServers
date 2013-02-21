package services.mvc.forms

import scala.language.implicitConversions
import services.http.forms.CustomerLoginForm
import models.frontend.login_page.LoginFormViewModel
import models.frontend.forms.Field
import controllers.WebsiteControllers

/**
 * Transforms [[services.http.forms.CustomerLoginForm]]s into their sister clas,
 * [[models.frontend.login_page.LoginFormViewModel]].
 */
object LoginFormViewConversions {

  import services.mvc.FormConversions._

  class LoginFormViewConvertor(model: CustomerLoginForm) {
    def asView: LoginFormViewModel = {
      val generalModelErrors = model.errorsOrValidatedForm.left.getOrElse(List())

      defaultView.copy(
        email = model.email.asViewField,
        password = model.password.asViewField,
        generalErrors = generalModelErrors.map(error => error.asViewError)
      )
    }
  }

  def defaultView: LoginFormViewModel = {
    import controllers.routes.WebsiteControllers.{getRecoverAccount, postLogin}
    import CustomerLoginForm.Fields

    LoginFormViewModel(
      email = Field(Fields.Email),
      password = Field(Fields.Password),
      generalErrors = List(),
      actionUrl=postLogin.url,
      forgotPasswordUrl=getRecoverAccount.url
    )
  }

  //
  // Implicit members
  //
  implicit def modelToConvertor(model: CustomerLoginForm): LoginFormViewConvertor = {
    new LoginFormViewConvertor(model)
  }
}
