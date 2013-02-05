package services.mvc.forms

import services.http.forms.AccountRegistrationForm
import models.frontend.login_page.AccountRegistrationFormViewModel
import models.frontend.forms.Field
import controllers.WebsiteControllers
//import controllers.website.consumer.PostRegisterConsumerForm
import views.html.helper.FieldElements

/**
 * Converts [[services.http.forms.AccountRegistrationForm]]s into their sister
 * view class, the [[models.frontend.login_page.AccountRegistrationFormViewModel]].
 */
object AccountRegistrationFormViewConversions {
//  import services.mvc.FormConversions._
//
//  class AccountRegistrationFormViewConvertor(model: PostRegisterConsumerForm) {
//    def asView: AccountRegistrationFormViewModel = {
//      defaultView.copy(
//        email=model.email.asViewField,
//        password=model.password.asViewField,
//        bulkEmail=model.bulkEmail.asViewField,
//        generalErrors=model.fieldInspecificErrors.map(error => error.asViewError)
//      )
//    }
//  }
//
//  def defaultView: AccountRegistrationFormViewModel = {
//    AccountRegistrationFormViewModel(
//      email = Field("email"),
//      password = Field("password"),
//      bulkEmail = Field("bulk-email"),
//      generalErrors = List(),
//      actionUrl = controllers.routes.WebsiteControllers.postRegisterConsumerEndpoint.url
//    )
//  }
//
//  //
//  // Implicit members
//  //
//  implicit def modelToConvertor(model: AccountRegistrationForm): AccountRegistrationFormViewConvertor = {
//    new AccountRegistrationFormViewConvertor(model)
//  }
}
