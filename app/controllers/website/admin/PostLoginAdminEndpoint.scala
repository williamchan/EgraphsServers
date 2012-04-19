package controllers.website.admin

import play.mvc.Controller
import play.mvc.results.Redirect
import services.Utils
import controllers.WebsiteControllers
import play.data.validation.Validation
import models.{Administrator, AdministratorStore}
import services.http.{SecurityRequestFilters, ControllerMethod}

private[controllers] trait PostLoginAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def administratorStore: AdministratorStore

  def postLoginAdmin(email: String, password: String) = controllerMethod() {

    securityFilters.checkAuthenticity {

      Validation.required("Email", email)
      Validation.email("Email", email)
      Validation.required("Password", password)

      var administrator: Option[Administrator] = None
      if (validationErrors.isEmpty) {
        administrator = administratorStore.authenticate(email = email, passwordAttempt = password)
        Validation.isTrue("Buddy, are you an administrator?", administrator.isDefined)
      }

      if (!validationErrors.isEmpty) {
        WebsiteControllers.redirectWithValidationErrors(GetLoginAdminEndpoint.url())

      } else {
        session.put(WebsiteControllers.adminIdKey, administrator.get.id.toString)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin").url)
      }
    }
  }

}
