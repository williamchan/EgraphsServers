package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import services.http.ControllerMethod
import models.{Administrator, AdministratorStore}

private[controllers] trait PostAdminLoginEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def administratorStore: AdministratorStore

  def postAdminLogin(email: String, password: String) = controllerMethod() {
    Validation.required("Email", email)
    Validation.email("Email", email)
    Validation.required("Password", password)

    var administrator: Option[Administrator] = None
    if (validationErrors.isEmpty) {
      administrator = administratorStore.authenticate(email = email, passwordAttempt = password)
      Validation.isTrue("Buddy, are you an administrator?", administrator.isDefined)
    }

    if (!validationErrors.isEmpty) {
      WebsiteControllers.redirectWithValidationErrors(GetAdminLoginEndpoint.url())

    } else {
      session.put(WebsiteControllers.adminIdKey, administrator.get.id.toString)
      new Redirect(Utils.lookupUrl("WebsiteControllers.getCelebrities").url)
    }
  }

}
