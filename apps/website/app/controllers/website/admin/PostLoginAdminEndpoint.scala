package controllers.website.admin

import play.api.mvc.{Action, Controller}
import models.AdministratorStore
import services.http.POSTControllerMethod
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.{Constraint, Invalid, Valid}
import services.http.EgraphsSession.Conversions._

private[controllers] trait PostLoginAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def administratorStore: AdministratorStore
  
  case class PostLoginAdminForm(email: String, password: String) {
    lazy val authenticatedAdminLogin = administratorStore.authenticate(email = email, passwordAttempt = password)
  }

  def postLoginAdmin = postController() {
    Action { implicit request =>
      val loginForm = Form(
        mapping(
            "email" -> email.verifying(nonEmpty),
            "password" -> text.verifying(nonEmpty)
        )(PostLoginAdminForm.apply)(PostLoginAdminForm.unapply)
          .verifying(isAdminLogin)
      )
        
      loginForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(controllers.routes.WebsiteControllers.getLoginAdmin).flashing("errors" -> formWithErrors.errors.head.message.toString())
          },
          validForm => {
            Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url).withSession(
              request.session.withAdminId(validForm.authenticatedAdminLogin.get.id))
          }
      )
    }
  }
  
  def isAdminLogin: Constraint[PostLoginAdminForm] = {
    Constraint { form: PostLoginAdminForm =>
      form.authenticatedAdminLogin match {
        case Some(a) => Valid
        case None => Invalid("Buddy are you an administrator?")
      }
    }
  }
}
