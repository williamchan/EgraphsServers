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
   * This Play Form Constraint takes an optional additional constraint in the form of a function. The Form is valid if
   * no account exists or the accountConstraint is met for the existing account. By default, no existing accounts are
   * valid.
   * Example usage:

   * def isValidNewCelebrityEmail: Constraint[String] = {
   *   isValidNewEmail(accountConstraint = { account: Account => !account.celebrityId.isDefined})
   * }
   *
   * @param accountConstraint
   * @return
   */
  def isValidNewEmail(accountConstraint: Account => Boolean = { account => false}): Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(preexistingAccount) if !accountConstraint(preexistingAccount) => Invalid("Account with e-mail address (" + email + ") already exists")
        case _ => Valid
      }
    }
  }

  /**
   * Similar to isValidNewEmail, takes an extra constraint that must evaluate to true for the form to be Valid.
   * @param accountConstraint
   * @return Valid if account exists for the given email and accountConstraint passes, else Invalid
   */
  def isValidExistingEmail(accountConstraint: Account => Boolean = { account => true}): Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(existingAccount) if accountConstraint(existingAccount) => Valid
        case _ => Invalid("Account not found for given email")
      }
    }
  }

  def isValidNewCustomerEmail: Constraint[String] = {
    isValidNewEmail(accountConstraint = {account: Account => !account.customerId.isDefined})
  }

  def isValidCustomerEmail: Constraint[String] = {
    isValidExistingEmail(accountConstraint = {account: Account => account.customerId.isDefined})
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