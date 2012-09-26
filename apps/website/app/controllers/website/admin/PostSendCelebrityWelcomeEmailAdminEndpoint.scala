package controllers.website.admin

import models._
import services.mail.TransactionalMail
import controllers.WebsiteControllers
import play.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}


trait PostSendCelebrityWelcomeEmailAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def transactionalMail: TransactionalMail

  def postSendCelebrityWelcomeEmailAdmin(celebrityId: Long, celebrityEmail: String) = postController() {
    adminFilters.requireAdministratorLogin { admin =>      
      val maybeSuccessfulRedirect = for (
        celebrity <- celebrityStore.findById(celebrityId)
      ) yield {
        celebrity.sendWelcomeEmail(celebrityEmail)
        WebsiteControllers.redirectWithValidationErrors(
          WebsiteControllers.reverse(WebsiteControllers.getCelebrityAdmin(celebrityId=celebrityId))
        )
      }
      maybeSuccessfulRedirect.getOrElse(NotFound("Celebrity with Id " + celebrityId + " not NotFound"))
    }
  }

}