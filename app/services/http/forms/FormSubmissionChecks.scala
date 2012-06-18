package services.http.forms

import com.google.inject.Inject
import play.data.validation.{Validation}
import play.data.binding.types.DateBinder
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import play.libs.I18N
import models.{Account, AccountStore}

class FormSubmissionChecks @Inject()(accountStore: AccountStore) {

  private def playValidation = Validation.current()

  def isInt(string: String, message: String="Valid integer required")
  : Either[FormError, Int] =
  {
    try {
      Right(string.toInt)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError(message))
    }
  }

  def isLong(toValidate: String, message: String="Valid number required")
  : Either[FormError, Long] =
  {
    try {
      Right(toValidate.toLong)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError(message))
    }
  }

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

  def isDate(toValidate: String, message: String="Properly formatted date required")
  : Either[FormError, Date] =
  {
    for (notI18nDate <- isDateWithFormat(I18N.getDateFormat, toValidate, message).left;
         notAnyKnownDate <- isDateWithFormat(DateBinder.ISO8601, toValidate, message).left)
      yield notAnyKnownDate
  }

  /** Grabbed wholesale from play.data.binding.types.DateBinder */
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

  def dependentFieldIsValid[ValueType](toValidate: FormField[ValueType])
  : Either[DependentFieldError, ValueType] =
  {
    for (validationError <- toValidate.validate.left) yield new DependentFieldError
  }

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

  def isCustomerAccount(toValidate: Account, message: String="Valid customer account required")
  : Either[FormError, Long] =
  {
    toValidate.customerId match {
      case None => Left(new SimpleFormError(message))
      case Some(id) => Right(id)
    }
  }

  def isEmailAddress(toValidate: String, message: String="Valid email address required")
  : Either[FormError, String] = {
    if (playValidation.email(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  def isPhoneNumber(toValidate: String, message: String = "Valid phone number required")
  : Either[FormError, String] =
  {
    if (playValidation.phone(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError(message))
    }
  }

  def isAtMost(maximum: Int, toValidate: Int, message: String = "Over maximum value")
  : Either[FormError, Int] =
  {
    if(toValidate > maximum) Left(new SimpleFormError(message)) else Right(toValidate)
  }

  def isAtLeast(minimum: Int, toValidate: Int, message: String = "Under minimum value")
  : Either[FormError, Int] =
  {
    if(toValidate < minimum) Left(new SimpleFormError(message)) else Right(toValidate)
  }

}

object FormSubmissionChecks {
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