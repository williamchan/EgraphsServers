package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import services.http.{SecurityRequestFilters, ControllerMethod}
import models._

private[controllers] trait PostLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def accountStore: AccountStore

  def postLogin(email: String, password: String) = controllerMethod() {

    securityFilters.checkAuthenticity {

      Validation.required("Email", email)
      Validation.email("Email", email)
      Validation.required("Password", password)

      val accountLoginAttempt: Either[AccountAuthenticationError, Account] = accountStore.authenticate(email, password)
      Validation.isTrue("The username or password did not match. Please try again.",
        accountLoginAttempt.isRight && accountLoginAttempt.right.get.customerId.isDefined)

      if (!validationErrors.isEmpty) {
        WebsiteControllers.redirectWithValidationErrors(GetLoginEndpoint.url())

      } else {
        session.put(WebsiteControllers.customerIdKey, accountLoginAttempt.right.get.customerId.get.toString)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
      }
    }
  }
}
