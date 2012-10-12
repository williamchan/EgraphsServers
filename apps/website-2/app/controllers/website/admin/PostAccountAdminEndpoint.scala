package controllers.website.admin

import models._
import play.api.mvc.Results.{Ok, Redirect}
import controllers.WebsiteControllers
import play.api.mvc.Controller
import play.data.validation._
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid

trait PostAccountAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def accountStore: AccountStore

  case class PostAccountForm(accountId: Long, email: String, password: String) {
    lazy val accountById = accountStore.findById(accountId)
    lazy val accountByEmail = accountStore.findByEmail(email)
  }

  /**
   * For updating an existing Account.
   */
  def postAccountAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      Action { implicit request =>
        val changeAccountForm = Form(
          mapping(
            "accountId" -> longNumber,
            "email" -> email.verifying(nonEmpty),
            "password" -> text.verifying(nonEmpty, passwordIsValid)
          )(PostAccountForm.apply)(PostAccountForm.unapply)
            .verifying(emailMatchesMatchesAccountOrIsUnique)
        )
        
        changeAccountForm.bindFromRequest.fold(
          errors => Ok("herpderp"),
          form => {
            val account = form.accountById.get
            account.copy(email = form.email).withPassword(form.password).right.get.save()

            Redirect(controllers.routes.WebsiteControllers.getAccountAdmin(form.accountId))
          }
        )
      }
    }
  }
  
  def emailMatchesMatchesAccountOrIsUnique: Constraint[PostAccountForm] = {
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
  
  def passwordIsValid: Constraint[String] = {
    Constraint { password: String =>
      Password.validate(password).fold(error => Invalid("Invalid password"), valid => Valid)
    }
  }
      /*val isCreate = (accountId == 0)
      validate(isCreate = isCreate, accountId = accountId, email = email, password = password)

      if (!validationErrors.isEmpty) {
        redirectWithValidationErrors(accountId, email)

      } else {
        if (isCreate) {
          Account(email = email).withPassword(password).right.get.save()

        } else {
          val preexistingAccount = accountStore.get(accountId)
          if (isUpdatingPassword(preexistingAccount, password)) {
            preexistingAccount.copy(email = email).withPassword(password).right.get.save()
          } else {
            preexistingAccount.copy(email = email).save()
          }
        }
        new Redirect(WebsiteControllers.reverse(WebsiteControllers.getAccountsAdmin(email)).url)
      }
    }*/

    
  

  /*private def isUpdatingPassword(account: Account, password: String): Boolean = {
    !(password.isEmpty || (account.password.isDefined && password == account.password.get.hash))
  }

  private def validate(isCreate: Boolean, accountId: Long, email: String, password: String) {
    Validation.required("Email", email)
    Validation.email("Email", email)
    Validation.required("Password", password)

    val accountByEmail = accountStore.findByEmail(email)

       val preexistingAccount = accountStore.get(accountId)

      val isEmailUnique = accountByEmail.isEmpty || accountByEmail.get.id == accountId
      Validation.isTrue("An account with that email address already exists", isEmailUnique)

      if (isUpdatingPassword(preexistingAccount, password)) {
        val passwordValidationOrAccount = preexistingAccount.withPassword(password)
        if (passwordValidationOrAccount.isLeft) Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)
      }
  }

  private def redirectWithValidationErrors(accountId: Long,
                                           email: String): Redirect = {
    val flash = play.mvc.Http.Context.current().flash()
    flash.put("accountId", accountId)
    flash.put("email", email)
    flash.put("password", "")
    if (accountId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateAccountAdminEndpoint.url())
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetAccountAdminEndpoint.url(accountId = accountId))
    }
  }*/
}
