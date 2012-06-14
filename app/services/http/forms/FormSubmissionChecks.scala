package services.http.forms

import com.google.inject.Inject
import play.data.validation.{Validation}

class FormSubmissionChecks @Inject()() {

  private def playValidation = Validation.current()

  def isInt(string: String): Either[FormError, Int] = {
    try {
      Right(string.toInt)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError("Valid integer required"))
    }
  }

  def isLong(toValidate: String): Either[FormError, Long] = {
    try {
      Right(toValidate.toLong)
    } catch {
      case _:NumberFormatException => Left(new SimpleFormError("Valid long required"))
    }
  }

  def isBoolean(toValidate: String): Either[FormError, Boolean] = {
    toValidate.toLowerCase match {
      case "1" | "true" | "on" | "yes" =>
        Right(true)
      case "0" | "false" | "off" | "no" =>
        Right(false)
      case _ =>
        Left(new SimpleFormError("Valid boolean value required"))
    }
  }

  def isEmailAddress(toValidate: String): Either[FormError, String] = {
    if (playValidation.email(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError("Valid email address required"))
    }
  }

  def isPhoneNumber(toValidate: String): Either[FormError, String] = {
    if (playValidation.phone(toValidate).ok) {
      Right(toValidate)
    } else {
      Left(new SimpleFormError("Valid phone number required"))
    }
  }
}
