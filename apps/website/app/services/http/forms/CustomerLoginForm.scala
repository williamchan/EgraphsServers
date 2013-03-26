package services.http.forms

import com.google.inject.Inject
import services.Utils

/**
 * Define the fields (and validations thereof) for logging in to our system as a customer.
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *     by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 **/
class CustomerLoginForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[CustomerLoginForm.Validated]
{
  import CustomerLoginForm.{Fields, badCredentialsMessage}

  //
  // Field values and validations
  //
  val email = field(Fields.Email).validatedBy { toValidate =>
    for (
      // Email or username is required to log in
      submitted <- check.isSomeValue(toValidate, "We're gonna need this").right;

      // Value's gotta be a valid email
      validEmail <- check.isEmailAddress(submitted).right
    ) yield {
      validEmail
    }
  }

  val password = field(Fields.Password).validatedBy { toValidate =>
    for (
      // Password is required to log in
      submitted <- check.isSomeValue(toValidate, "We're gonna need this").right;

      // Needs to match up to our internal metric for passwords
      validPassword <- check.isValidPassword(submitted).right
    ) yield {
      validPassword
    }
  }

  // customerId is a field derived from the other two, which means it has no entry
  // in the paramsMap
  val customerId = field.validatedBy { toValidate =>
    for (
      // Submitted email had to be valid
      validEmail <- check.dependentFieldIsValid(email).right;

      // Submitted password had to be valid
      validPassword <- check.dependentFieldIsValid(password).right;

      // Email and password had to match up to a known account
      validAccount <- check.isValidAccount(validEmail, validPassword, badCredentialsMessage).right;

      // That known account had to have a customer face.
      customerId <- check.isCustomerAccount(validAccount, badCredentialsMessage).right
    ) yield {
      customerId
    }
  }

  //
  // Form[ValidatedCustomerLoginForm] members
  //
  protected def formAssumingValid: CustomerLoginForm.Validated = {
    // Safely access the account value in here
    CustomerLoginForm.Validated(customerId.value.get, email.value.get)
  }
}


object CustomerLoginForm {
  val badCredentialsMessage = "The login and password did not match. Try again?"

  object Fields {
    val Email = "login.email"
    val Password = "login.password"
  }

  /** Class to which the fully validated CustomerLoginForm resolves */
  case class Validated(customerId: Long, email: String)
}


/**
 * Factory that reads the CustomerLoginForm from either the request parameters (for a POST)
 * or the flash (for a redirected GET)
 *
 * @param formChecks checks used to validate the POST.
 */
class CustomerLoginFormFactory @Inject()(formChecks: FormChecks)
  extends ReadsForm[CustomerLoginForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): CustomerLoginForm = {
    new CustomerLoginForm(readable, formChecks)
  }

  //
  // ReadsForm[CustomerLoginForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): CustomerLoginForm = {
    apply(readable)
  }
}
