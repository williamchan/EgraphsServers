package services.http.forms

import com.google.inject.Inject
import models.{Account, Password, AccountStore}
import models.Password.{PasswordRequired, PasswordTooShort}
import play.api.data.validation.Constraint
import play.api.data.validation.{ Valid, Invalid }

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

  def isValidEmail(constraint: Account => Boolean = { account => true}): Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case None => Invalid("Account not found for given email")
        case Some(_) => Valid
      }
    }
  }

  def isValidNewCustomerEmail: Constraint[String] = {
    isValidNewEmail(constraint = {account: Account => account.customerId.isDefined})
  }

  def isValidCustomerEmail: Constraint[String] = {
    isValidEmail(constraint = {account: Account => account.customerId.isDefined})
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

  /**
   * Returns whether or not the email and password corresponded to actual customer account credentials.
   */
  def isValidCustomerAccount(email: String, password: String): Boolean = {
    accountStore.authenticate(email, password) match {
      case Left(_) => false
      case Right(account) => {
        account.customerId match {
          case None => false
          case Some(account) => true
        }
      }
    }
  }
}