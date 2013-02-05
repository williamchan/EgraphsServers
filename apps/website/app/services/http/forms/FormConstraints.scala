package services.http.forms

import com.google.inject.Inject
import play.api.data.validation.Constraint
import models.Password
import models.AccountStore
import play.api.data.validation.{ Valid, Invalid }

class FormConstraints @Inject() (accountStore: AccountStore) {
  def isUniqueEmail: Constraint[String] = {
    Constraint { email: String =>
      accountStore.findByEmail(email) match {
        case Some(preexistingAccount) if (preexistingAccount.customerId.isDefined) => Invalid("Customer with e-mail address already exists")
        case _ => Valid
      }
    }
  }

  def isPasswordValid: Constraint[String] = {
    Constraint { password: String =>
      if (Password.validate(password).isRight) Valid else Invalid("Password is invalid")
    }
  }
}