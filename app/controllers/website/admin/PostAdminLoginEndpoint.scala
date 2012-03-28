package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import models.AdministratorStore
import services.http.ControllerMethod

private[controllers] trait PostAdminLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def administratorStore: AdministratorStore

  def postAdminLogin(email: String, password: String): Redirect = controllerMethod() {
    Validation.required("Email", email)
    Validation.email("Email", email)
    Validation.required("Password", password)
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetAdminLoginEndpoint.url())
    }

    val administrator = administratorStore.authenticate(email = email, passwordAttempt = password)
    Validation.isTrue("Damnit you're not an administrator. Damnit.", administrator.isDefined)
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetAdminLoginEndpoint.url())
    }

    new Redirect(Utils.lookupUrl("WebsiteControllers.getCelebrities").url)
  }

}
