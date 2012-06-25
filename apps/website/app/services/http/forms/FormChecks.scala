package services.http.forms

import com.google.inject.Inject
import play.data.validation.{Validation}
import play.data.binding.types.DateBinder
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import play.libs.I18N
import models.{ProductStore, Account, AccountStore, Product}
import services.Utils

/**
 * A set of checks used by [[services.http.forms.Form]] to validate its
 * fields and return custom error messages.
 *
 * @param accountStore the store for Accounts.
 */
class FormChecks @Inject()(accountStore: AccountStore, productStore: ProductStore) {

  def isSomeValue[T](toValidate: Option[T], message: String="Was not a value")
  : Either[FormError, T] =
  {
    toValidate.map(value => Right(value)).getOrElse(error(message))
  }


  /**
   * Returns an Integer if the String could be turned into one.
   */
  def isInt(string: String, message: String="Valid integer required")
  : Either[FormError, Int] =
  {
    try {
      Right(string.toInt)
    } catch {
      case _:NumberFormatException => error(message)
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
      case _:NumberFormatException => error(message)
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
        error(message)
    }
  }

  // If the string was present it checks for the positive value,
  // but if it was not present it defaults to false. This is because of the wya
  // that forms work.
  def isChecked(toValidate: Option[String], message:String="Was not checked")
  : Either[FormError, Boolean] =
  {
    toValidate.map(string => isBoolean(string, message)).getOrElse(Right(false))
  }

  def isTrue(toValidate:Boolean, message: String="Should have been true")
  : Either[FormError, Boolean] = {
    if (toValidate) Right(toValidate) else error(message)
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
        error(message)
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
      case Left(_) => error(message)
      case Right(account) => Right(account)
    }
  }

  def isProductId(productId: Long, message: String="Product ID was invalid"): Either[FormError, Product] = {
    productStore.findById(productId).map(prod => Right(prod)).getOrElse {
      error(message)
    }
  }


  /**
   * Returns the customer ID if the provided account had a customer face
   */
  def isCustomerAccount(toValidate: Account, message: String="Valid customer account required")
  : Either[FormError, Long] =
  {
    toValidate.customerId match {
      case None => error(message)
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
      error(message)
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
      error(message)
    }
  }

  /**
   * Returns the provided number unaltered if it was no greater than the provided `maximum`
   */
  def isAtMost(maximum: Int, toValidate: Int, message: String = "Over maximum value")
  : Either[FormError, Int] =
  {
    if(toValidate > maximum) error(message) else Right(toValidate)
  }

  /**
   * Returns the provided number unaltered if it was no less than the provided `minimum`
   */
  def isAtLeast(minimum: Int, toValidate: Int, message: String = "Under minimum value")
  : Either[FormError, Int] =
  {
    if(toValidate < minimum) error(message) else Right(toValidate)
  }

  //
  // Private members
  //
  private def playValidation = {
    Validation.current()
  }

  private def error(message: String): Left[FormError, Nothing] = {
    Left(new SimpleFormError(message))
  }
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