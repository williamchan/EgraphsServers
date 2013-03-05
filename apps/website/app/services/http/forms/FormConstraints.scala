package services.http.forms

import com.google.inject.Inject
import play.api.data.validation.Constraint
import models.{Account, Password, AccountStore}
import play.api.data.validation.{ Valid, Invalid }
import models.Password.{PasswordRequired, PasswordTooShort}

class FormConstraints @Inject() (accountStore: AccountStore) {
   // TODO: think about making a composable constraint class, so that we can cascade errors. An implementation similar to
   // filters is an idea. See this discussion: https://github.com/Egraphs/eGraphsServers/pull/369/files#L2L13

  /**
   * This constraint takes an optional additional constraint in the form of a function. If and only if the function
   * returns true, then the email is invalid, so the constraint is an AND on the filters, limiting the scope of the
   * original constraint.
   * Example usage:
   *
   * def isValidNewCelebrityEmail: Constraint[String] = {
   *   isValidNewEmail(constraint = { account: Account => account.celebrityId.isDefined})
   * }
   *
   * @param constraint
   * @return
   */

  def isValidNewEmail(constraint: Account => Boolean = { account => true}): Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(preexistingAccount) if constraint(preexistingAccount) => Invalid("Account with e-mail address (" + email + ") already exists")
        case _ => Valid
      }
    }
  }

  def isValidNewCustomerEmail: Constraint[String] = {
    isValidNewEmail(constraint = {account: Account => account.customerId.isDefined})
  }

  def isPasswordValid: Constraint[String] = {
    Constraint { password: String =>
      Password.validate(password) match {
        case Right(_) => Valid
        case Left(PasswordTooShort(_)) => Invalid(Account.minPasswordLength + " characters minimum")
        case Left(PasswordRequired) => Invalid("Password is required")
      }
    }
  }
}