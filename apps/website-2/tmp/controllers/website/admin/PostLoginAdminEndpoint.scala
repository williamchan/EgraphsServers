package controllers.website.admin

import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import play.data.validation.Validation
import models.{Administrator, AdministratorStore}
import services.http.POSTControllerMethod

private[controllers] trait PostLoginAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def administratorStore: AdministratorStore

  def postLoginAdmin(email: String, password: String) = postController() {
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
      new Redirect(WebsiteControllers.reverse(WebsiteControllers.getCelebritiesAdmin()).url)
    }
  }
}
