package controllers.website.admin

import models._
import services.mail.TransactionalMail
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import models.{Administrator, AdministratorStore}
import services.http.{EgraphsSession, POSTControllerMethod}
import services.http.filters.HttpFilters
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import services.http.EgraphsSession._

trait PostSendCelebrityWelcomeEmailAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def transactionalMail: TransactionalMail

  def postSendCelebrityWelcomeEmailAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { celebrity =>
        Action { implicit request => 
          val emailForm = Form(single("celebrityEmail" -> email.verifying(nonEmpty)))
          emailForm.bindFromRequest.fold(
              formWithErrors => {
                Redirect(controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrityId = celebrityId)).flashing("errors" -> formWithErrors.errors.head.message.toString())
              },
              emailAddress => {
                celebrity.sendWelcomeEmail(emailAddress, bccEmail = Some(adminAccount.email))
                Redirect(controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrityId = celebrityId))
              })
        }
      }
    }
  }
}