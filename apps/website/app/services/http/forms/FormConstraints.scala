package services.http.forms

import com.google.inject.Inject
import play.api.data.validation.Constraint
import models.{Account, Password, AccountStore}
import play.api.data.validation.{ Valid, Invalid }
import models.Password.{PasswordRequired, PasswordTooShort}

class FormConstraints @Inject() (accountStore: AccountStore) {
  def isUniqueEmail: Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(preexistingAccount) if (preexistingAccount.customerId.isDefined || preexistingAccount.celebrityId.isDefined || preexistingAccount.administratorId.isDefined) => Invalid("Account with e-mail address already exists")
        case _ => Valid
      }
    }
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