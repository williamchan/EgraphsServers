package controllers.website.admin

import models._
import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.mvc.Controller
import play.data.validation._
import services.http.{SecurityRequestFilters, AdminRequestFilters, ControllerMethod}
import services.Utils

trait PostAccountEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def securityFilters: SecurityRequestFilters
  protected def adminFilters: AdminRequestFilters
  protected def accountStore: AccountStore

  /**
   * For updating an existing Account.
   */
  def postAccount(accountId: Long = 0,
                  email: String,
                  password: String) = controllerMethod() {

    securityFilters.checkAuthenticity {
      adminFilters.requireAdministratorLogin { admin =>
        val isCreate = (accountId == 0)
        validate(isCreate = isCreate, accountId = accountId, email = email, password = password)

        if (!validationErrors.isEmpty) {
          redirectWithValidationErrors(accountId, email)

        } else {
          if (isCreate) {
            Account(email = email).withPassword(password).right.get.save()

          } else {
            val preexistingAccount = accountStore.findById(accountId).get
            if (isUpdatingPassword(preexistingAccount, password)) {
              preexistingAccount.copy(email = email).withPassword(password).right.get.save()
            } else {
              preexistingAccount.copy(email = email).save()
            }
          }
          new Redirect(Utils.lookupUrl("WebsiteControllers.getAccounts", Map("email" -> email)).url)
        }
      }
    }
  }

  private def isUpdatingPassword(account: Account, password: String): Boolean = {
    !(password.isEmpty || (account.password.isDefined && password == account.password.get.hash))
  }

  private def validate(isCreate: Boolean, accountId: Long, email: String, password: String) {
    Validation.required("Email", email)
    Validation.email("Email", email)
    Validation.required("Password", password)

    val accountByEmail = accountStore.findByEmail(email)

    if (isCreate) {
      val isEmailUnique = accountByEmail.isEmpty
      Validation.isTrue("An account with that email address already exists", isEmailUnique)

      val passwordValidationOrAccount = Account().withPassword(password)
      if (passwordValidationOrAccount.isLeft) Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)

    } else {
      val preexistingAccount = accountStore.findById(accountId).get

      val isEmailUnique = accountByEmail.isEmpty || accountByEmail.get.id == accountId
      Validation.isTrue("An account with that email address already exists", isEmailUnique)

      if (isUpdatingPassword(preexistingAccount, password)) {
        val passwordValidationOrAccount = preexistingAccount.withPassword(password)
        if (passwordValidationOrAccount.isLeft) Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)
      }
    }
  }

  private def redirectWithValidationErrors(accountId: Long,
                                           email: String): Redirect = {
    flash.put("accountId", accountId)
    flash.put("email", email)
    flash.put("password", "")
    if (accountId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateAccountEndpoint.url())
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetUpdateAccountEndpoint.url(accountId = accountId))
    }
  }
}
