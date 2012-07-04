package services.http.forms

import models.{Password, Customer, Account}
import services.Utils
import com.google.inject.Inject

/**
 * Define the fields (and validations thereof) for the form transmitted by this endpoint
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *     by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 **/

class AccountVerificationForm(val paramsMap: Form.Readable, check: FormChecks) extends Form[AccountVerificationForm.Validated]
{
  import AccountVerificationForm.Fields

  val secretKey = new RequiredField[String](Fields.SecretKey.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  val email = new RequiredField[String](Fields.Email.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  val newPassword = new RequiredField[String](Fields.NewPassword.name) {
    def validateIfPresent = {
      Password.validate(stringToValidate) match {
        case Left(validationResult) => Left(new SimpleFormError("Password must be at least 8 characters"))
        case _ => Right(stringToValidate) }
    }
  }
  val passwordConfirm = new RequiredField[String](Fields.PasswordConfirm.name) {
    def validateIfPresent = {
      newPassword.value match {
        case Some(password) if password == stringToValidate => Right(stringToValidate)
        case _ => Left(new SimpleFormError("Passwords do not match"))
      }
    }
  }

  //
  // Form[ValidatedAccountVerificationForm] members
  //

  protected def formAssumingValid: AccountVerificationForm.Validated = {
    // Safely access the account value in here

    AccountVerificationForm.Validated(
      newPassword = newPassword.value.get,
      passwordConfirm = passwordConfirm.value.get,
      secretKey = secretKey.value.get,
      email = email.value.get
    )
  }
}

object AccountVerificationForm {
  object Fields extends Utils.Enum {
    sealed case class EnumVal(name: String) extends Value

    // Strings need to match up with param names
    val NewPassword = EnumVal("newPassword")
    val PasswordConfirm = EnumVal("passwordConfirm")
    val Email = EnumVal("email")
    val SecretKey = EnumVal("secretKey")
  }

  case class Validated(newPassword: String, passwordConfirm: String, email: String, secretKey: String)
}

class AccountVerificationFormFactory @Inject()(formChecks: FormChecks)
  extends ReadsForm[AccountVerificationForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): AccountVerificationForm = {
    new AccountVerificationForm(readable, formChecks)
  }

  //
  // ReadsForm[AccountVerificationForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): AccountVerificationForm = {
    apply(readable)
  }
}
