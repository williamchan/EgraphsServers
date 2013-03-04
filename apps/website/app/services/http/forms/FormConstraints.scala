package services.http.forms

import com.google.inject.Inject
import play.api.data.validation.Constraint
import models.{Account, Password, AccountStore}
import play.api.data.validation.{ Valid, Invalid }
import models.Password.{PasswordRequired, PasswordTooShort}

class FormConstraints @Inject() (accountStore: AccountStore) {
  /**
   * This constraint takes an optional additional constraint in the form of a function. If and only if the function
   * returns true, then the email is invalid, so the constraint is an AND on the filters, limiting the scope of the
   * original constraint.
   * Example usage:
   *
   * def isUniqueCelebrityEmail: Constraint[String] = {
   *   isUniqueEmail(constraint = { account: Account => account.celebrityId.isDefined})
   * }
   *
   * @param constraint
   * @return
   */
  def isUniqueEmail(constraint: Account => Boolean = { account => true} ): Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(preexistingAccount) if constraint(preexistingAccount) => Invalid("Account with e-mail address already exists")
        case _ => Valid
      }
    }
  }

  def isUniqueCustomerEmail: Constraint[String] = {
    isUniqueEmail(constraint = { account: Account => account.customerId.isDefined })
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