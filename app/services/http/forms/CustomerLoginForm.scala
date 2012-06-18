package services.http.forms

import com.google.inject.Inject
import services.Utils

/**
 * Define the fields (and validations thereof) for the form transmitted by this endpoint
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *     by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 **/
class CustomerLoginForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[CustomerLoginForm.Validated]
{
  import CustomerLoginForm.Fields

  //
  // Field values and validations
  //
  val email = new RequiredField[String](Fields.Email.name) {
    def validateIfPresent = {
      for (validEmail <- check.isEmailAddress(stringToValidate).right) yield validEmail
    }
  }

  val password = new RequiredField[String](Fields.Password.name) {
    def validateIfPresent = {
      Right(stringToValidate)
    }
  }

  // customerId is a field derived from the other two, which means it has no entry
  // in the paramsMap
  val customerId = new DerivedField[Long] {
    def validate = {
      for (validEmail <- check.dependentFieldIsValid(email).right;
           validPassword <- check.dependentFieldIsValid(password).right;
           validAccount <- check.isValidAccount(validEmail,
             validPassword,
             badCredentialsMessage).right;
           customerId <- check.isCustomerAccount(validAccount, badCredentialsMessage).right)
      yield {
        customerId
      }
    }
  }

  //
  // Form[ValidatedCustomerLoginForm] members
  //
  protected def formAssumingValid: CustomerLoginForm.Validated = {
    // Safely access the account value in here
    CustomerLoginForm.Validated(customerId.value.get)
  }

  //
  // Private members
  //
  private val badCredentialsMessage = "The username and password did not match. Please try again"
}


object CustomerLoginForm {

  object Fields extends Utils.Enum {
    sealed case class EnumVal(name: String) extends Value

    val Email = EnumVal("login.email")
    val Password = EnumVal("login.password")
  }

  /** Class to which the fully validated CustomerLoginForm resolves */
  case class Validated(customerId: Long)
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
