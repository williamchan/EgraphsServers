package controllers.website.admin

import play.data.validation._
import models._
import enums.PublishedStatus
import play.mvc.results.Redirect
import services.mail.TransactionalMail
import controllers.WebsiteControllers
import play.mvc.Controller
import play.data.validation.Validation.ValidationResult
import services.blobs.Blobs.Conversions._
import java.io.File
import services.{ImageUtil, Utils}
import services.http.{POSTControllerMethod, AdminRequestFilters, CelebrityAccountRequestFilters}
import org.apache.commons.mail.HtmlEmail

trait PostSendCelebrityWelcomeEmailAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def transactionalMail: TransactionalMail

  def postSendCelebrityWelcomeEmailAdmin(celebrityId: Long) = postController() {
    adminFilters.requireAdministratorLogin { admin =>      
      val maybeSuccessfulRedirect = for (
        celebrity <- celebrityStore.findById(celebrityId)
      ) yield {
        celebrity.sendWelcomeEmail()
        WebsiteControllers.redirectWithValidationErrors(GetCelebrityAdminEndpoint.url(celebrityId = celebrityId))
      }
      maybeSuccessfulRedirect.getOrElse(NotFound("Celebrity with Id " + celebrityId + " not NotFound"))
    }
  }
}