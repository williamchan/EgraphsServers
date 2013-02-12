package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import services.http.forms.FormConstraints

trait PostAccountAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore
  protected def formConstraints: FormConstraints

  case class PostAccountForm(accountId: Long, email: String, password: String) {
    lazy val accountById = accountStore.findById(accountId)
    lazy val accountByEmail = accountStore.findByEmail(email)
  }

  /**
   * For updating an existing Account.
   */
  def postAccountAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val changeAccountForm = Form(
          mapping(
            "accountId" -> longNumber,
            "email" -> email.verifying(nonEmpty),
            "password" -> text.verifying(nonEmpty, formConstraints.isPasswordValid)
          )(PostAccountForm.apply)(PostAccountForm.unapply)
            .verifying(emailMatchesAccountOrIsUnique)
        )
        
        changeAccountForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(controllers.routes.WebsiteControllers.getAccountAdmin(formWithErrors.get.accountId)).flashing("errors" -> formWithErrors.errors.head.message.toString())
          },
          validForm => {
            val account = validForm.accountById.get
            account.copy(email = validForm.email).withPassword(validForm.password).right.get.save()
            Redirect(controllers.routes.WebsiteControllers.getAccountsAdmin)
          }
        )
      }
    }
  }
  
  def emailMatchesAccountOrIsUnique: Constraint[PostAccountForm] = {
    Constraint { form: PostAccountForm =>
      val maybeValid = form.accountById.flatMap { accountById =>
        val isNotChangingEmail = accountById.email == form.email
        if (isNotChangingEmail || form.accountByEmail.isEmpty) {
          Some(Valid)
        } else {
          None
        }
      }

      maybeValid.getOrElse(Invalid("An account with that email address already exists"))
    }
  }
}
