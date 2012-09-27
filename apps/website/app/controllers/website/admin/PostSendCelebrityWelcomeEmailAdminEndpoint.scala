package controllers.website.admin

import models._
import services.mail.TransactionalMail
import controllers.WebsiteControllers
import play.mvc.Controller
import services.http.{CelebrityAccountRequestFilters, POSTControllerMethod, AdminRequestFilters}
import play.mvc.results.Redirect
import play.data.validation.Validation


trait PostSendCelebrityWelcomeEmailAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def transactionalMail: TransactionalMail

  def postSendCelebrityWelcomeEmailAdmin(celebrityEmail: String) = postController() {
    adminFilters.requireAdministratorLogin { admin =>
      celebFilters.requireCelebrityId(request) { celebrity =>
        Validation.required("E-mail address", celebrityEmail)
        Validation.email("E-mail address", celebrityEmail)
        if (validationErrors.isEmpty) {
          celebrity.sendWelcomeEmail(celebrityEmail, bccEmail = Some(admin.account.email))
          new Redirect(WebsiteControllers.reverse(WebsiteControllers.getCelebrityAdmin(celebrityId = celebrity.id)).url)
        } else {
          WebsiteControllers.redirectWithValidationErrors(WebsiteControllers.reverse(WebsiteControllers.getCelebrityAdmin(celebrityId = celebrity.id)))
        }
      }
    }
  }

}