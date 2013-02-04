package services.http.forms

/**
 * Define the fields (and validations thereof) for account registration.
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *                  by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 **/
class AccountRegistrationForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[AccountRegistrationForm.Valid] {

  import AccountRegistrationForm.Params

  //
  // Field values and validations
  //
  val email = field(Params.Email).validatedBy { toValidate =>
    for (
      // There's gotta be a value
      submission <- check.isSomeValue(toValidate, "We're gonna need this").right;

      // Value's gotta be a valid email address
      email <- check.isEmailAddress(submission, "Not an e-mail address").right;

      // The email address has to be unique in our system.
      unique <- check.isUniqueEmail(email, "Aw snap, this e-mail is taken!").right
    ) yield {
      unique
    }
  }

  val password = field(Params.Password).validatedBy { toValidate =>
    for (
      // There's gotta be a value
      submission <- check.isSomeValue(toValidate, "We're gonna need this").right;

      // Value's gotta be a valid password
      validPassword <- check.isValidPassword(submission).right
    ) yield {
      validPassword
    }
  }

  val bulkEmail = field(Params.BulkEmail).validatedBy { toValidate =>
    for (
      // There's gotta be a value
      submission <- check.isSomeValue(toValidate, "We're gonna need this").right;

      // Value's gotta be a valid password
      validBulkEmail <- check.isBoolean(submission).right
    ) yield {
      validBulkEmail
    }
  }

  //
  // Form members
  //
  protected def formAssumingValid: AccountRegistrationForm.Valid = {
    AccountRegistrationForm.Valid(
      email.value.get,
      password.value.get,
      bulkEmail.value.get
    )
  }
}


object AccountRegistrationForm {
  object Params {
    val Email = "registration.email"
    val Password = "registration.password"
    val BulkEmail = "registration.bulk_email"
  }

  /**
   * Fully validated version of the AccountRegistrationForm
   * @param email the submitted email for registration. We know due to validation
   *     that it is not a duplciate
   * @param password the submitted password. We know due to validation that it passes
   *     our (not so stringent) strength tests.
   * @param bulkEmail This is true if we are signing them up for the bulk mailing list.
   */
  case class Valid(email: String, password: String, bulkEmail: Boolean)
}