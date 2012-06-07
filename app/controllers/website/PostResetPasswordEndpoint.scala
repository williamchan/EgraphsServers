package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import services.http.{SecurityRequestFilters, ControllerMethod}
import models.AccountStore

private[controllers] trait PostResetPasswordEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def accountStore: AccountStore

  def postResetPassword(email: String, password: String, password2: String) = controllerMethod() {

    securityFilters.checkAuthenticity {

      Validation.required("Password", password)
      Validation.isTrue("Passwords do not match", password == password2)
      val account = accountStore.findByEmail(email).get
      val passwordValidationOrAccount = account.withPassword(password)
      for (passwordValidation <- passwordValidationOrAccount.left) {
        Validation.addError("Password", passwordValidation.error.toString)
      }

      if (!validationErrors.isEmpty) {
        WebsiteControllers.redirectWithValidationErrors(GetResetPasswordEndpoint.url(account.id, account.resetPasswordKey.get))

      } else {

        passwordValidationOrAccount.right.get.save()
        new Redirect(Utils.lookupUrl("WebsiteControllers.getLogin").url)
      }
    }
  }
}
