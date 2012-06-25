package services.http.forms

import com.google.inject.Inject
import play.data.validation.{Validation}
import play.data.binding.types.DateBinder
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import play.libs.I18N
import models.{CustomerStore, Account, AccountStore}

/**
 * A set of checks used by [[services.http.forms.Form]] to validate its
 * fields and return custom error messages.
 *
 * @param accountStore the store for Accounts.
 */
class FormChecks @Inject()(accountStore: AccountStore, customerStore: CustomerStore) {

  /**
   * Returns an Integer if the String could be turned into one.
   */
  def isInt(string: String, message: String="Valid integer required")
  : Either[FormError, Int] =
  {
    try {
      Right(string.toInt)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns a Long if the string could be turned into one.
   */
  def isLong(toValidate: String, message: String="Valid number required")
  : Either[FormError, Long] =
  {
    try {
      Right(toValidate.toLong)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns a Boolean if the string could be turned into one
   */
  def isBoolean(toValidate: String, message: String="Valid boolean required")
  : Either[FormError, Boolean] =
  {
    toValidate.trim.toLowerCase match {
      case "1" | "true" | "on" | "yes" =>
        Right(true)

      case "0" | "false" | "off" | "no" =>
        Right(false)

      case _ =>
        Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns a Date if the string could be turned into one. It checks against the
   * local I18N date type and the ISO8601 date type.
   */
  def isDate(toValidate: String, message: String="Properly formatted date required")
  : Either[FormError, Date] =
  {
    for (notI18nDate <- isDateWithFormat(I18N.getDateFormat, toValidate, message).left;
         notAnyKnownDate <- isDateWithFormat(DateBinder.ISO8601, toValidate, message).left)
      yield notAnyKnownDate
  }

  /**
   * Returns a Date if the String could be turned into one with the provided format
   * String. See [[java.text.SimpleDateFormat]] for example formats.
   *
   * Grabbed wholesale from play.data.binding.types.DateBinder.
   */
  def isDateWithFormat(
    format: String,
    toValidate: String,
    message: String="Properly formatted date required")
  : Either[FormError, Date] =
  {
    try {
      val dateFormat = new SimpleDateFormat(format)
      dateFormat.setLenient(false)
      Right(dateFormat.parse(toValidate))
    } catch {
      case _:ParseException =>
        Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns the field's value type if the provided FormField was valid.
   */
  def dependentFieldIsValid[ValueType](toValidate: FormField[ValueType])
  : Either[DependentFieldError, ValueType] =
  {
    for (validationError <- toValidate.validate.left) yield new DependentFieldError
  }

  /**
   * Returns the Account if the email and password corresponded to actual account
   * credentials.
   */
  def isValidAccount(
    email: String,
    password: String,
    message: String="Username or password did not match")
  : Either[FormError, Account] =
  {
    accountStore.authenticate(email, password) match {
      case Left(_) => Left(new SimpleFormError(message))
      case Right(account) => Right(account)
    }
  }

  /**
   * Returns the customer ID if the provided account had a customer face
   */
  def isCustomerAccount(toValidate: Account, message: String="Valid customer account required")
  : Either[FormError, Long] =
  {
    toValidate.customerId match {
      case None => Left(new SimpleFormError(message))
      case Some(id) => Right(id)
    }
  }

  /**
   * Returns the provided string unaltered if it was a valid email address
   * according to Play's built-in regex.
   */
  def isEmailAddress(toValidate: String, message: String="Valid email address required")
  : Either[FormError, String] = {
    if (playValidation.email(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  def isAlphaNumeric(toValidate: String, message: String="Must be alphanumeric")
  : Either[FormError, String] = {
    if (playValidation.`match`(toValidate, "[a-zA-Z0-9]*").ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns the provided string unaltered if it was a valid phone number according
   * to Play.
   */
  def isPhoneNumber(toValidate: String, message: String = "Valid phone number required")
  : Either[FormError, String] =
  {
    if (playValidation.phone(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  /**
   * Returns the provided number unaltered if it was no greater than the provided `maximum`
   */
  def isAtMost(maximum: Int, toValidate: Int, message: String = "Over maximum value")
  : Either[FormError, Int] =
  {
    if(toValidate > maximum) Left(new SimpleFormError(message)) else Right(toValidate)
  }

  /**
   * Returns the provided number unaltered if it was no less than the provided `minimum`
   */
  def isAtLeast(minimum: Int, toValidate: Int, message: String = "Under minimum value")
  : Either[FormError, Int] =
  {
    if(toValidate < minimum) Left(new SimpleFormError(message)) else Right(toValidate)
  }

  def isUniqueEmail(toValidate: String, message: String = "Unique email required")
  : Either[FormError, String] = {
    if (accountStore.findByEmail(toValidate).isEmpty) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  def isUniqueUsername(toValidate: String, message: String = "Unique username required")
  : Either[FormError, String] = {
    if (customerStore.findByUsername(toValidate).isEmpty) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  //
  // Private members
  //
  private def playValidation = Validation.current()
}

object FormChecks {
  /**
   * Returns the provided strings unaltered if there was at least one non-null
   * non-empty string in them.
   */
  def isPresent(toValidate: Iterable[String])
  : Either[ValueNotPresentFieldError, Iterable[String]] =
  {
    toValidate match {
      case null =>
        Left(ValueNotPresentFieldError())

      case strings if (strings.isEmpty ||
                        (strings.size == 1 && (strings.head == "" || strings.head == null))) =>
        Left(ValueNotPresentFieldError())

      case strings =>
        Right(strings)
    }
  }
}