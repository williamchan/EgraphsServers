package services.mvc.forms

import services.http.forms.AccountRegistrationForm
import models.frontend.login_page.AccountRegistrationFormViewModel
import models.frontend.forms.Field
import controllers.WebsiteControllers

/**
 * Converts [[services.http.forms.AccountRegistrationForm]]s into their sister
 * view class, the [[models.frontend.login_page.AccountRegistrationFormViewModel]].
 */
object AccountRegistrationFormViewConversions {
  import services.mvc.FormConversions._

  class AccountRegistrationFormViewConvertor(model: AccountRegistrationForm) {
    def asView: AccountRegistrationFormViewModel = {
      defaultView.copy(
        email=model.email.asViewField,
        password=model.password.asViewField,
        generalErrors=model.fieldInspecificErrors.map(error => error.asViewError)
      )
    }
  }

  def defaultView: AccountRegistrationFormViewModel = {
    import AccountRegistrationForm.Params
    import WebsiteControllers.{reverse, postRegisterConsumerEndpoint}

    AccountRegistrationFormViewModel(
      email=Field(Params.Email),
      password=Field(Params.Password),
      generalErrors=List(),
      actionUrl=reverse(postRegisterConsumerEndpoint).url
    )
  }

  //
  // Implicit members
  //
  implicit def modelToConvertor(model: AccountRegistrationForm): AccountRegistrationFormViewConvertor = {
    new AccountRegistrationFormViewConvertor(model)
  }
}
