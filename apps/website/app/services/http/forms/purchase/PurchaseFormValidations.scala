package services.http.forms.purchase

import services.http.forms.{FormChecks, FormError}
import models.enums.{WrittenMessageChoice, RecipientChoice, PrintingOption}
import models.enums.WrittenMessageChoice

class PurchaseFormValidation(toValidate: Iterable[String], check: FormChecks) {
  import PurchaseFormValidations._

  def validateHighQualityPrintField: Either[FormError, PrintingOption] = {
    for (
      param <- check.isSomeValue(toValidate, "Required").right;
      printingChoice <- check.isSomeValue(
                          PrintingOption(param),
                          "Was not a valid printing option"
                        ).right
    ) yield {
      printingChoice
    }
  }

  def validateSelectProductField: Either[FormError, models.Product] ={
    for (
      param <- check.isSomeValue(toValidate, "Required").right;
      paramAsLong <- check.isLong(param).right;
      product <- check.isProductId(paramAsLong).right
    ) yield {
      product
    }
  }

  def validateRecipientChoiceField
  : Either[FormError, RecipientChoice] =
  {
    for (
      param <- check.isSomeValue(toValidate, "Required").right;
      recipientChoice <- check.isSomeValue(RecipientChoice(param), "Invalid recipient choice").right
    ) yield {
      recipientChoice
    }
  }

  def validateRecipientNameField: Either[FormError, String] = {
    for (
      name <- check.isSomeValue(toValidate, "Required").right;
      _ <- check.isBetweenInclusive(2, 30, name.length, nameLengthErrorString).right
    ) yield {
      name
    }
  }

  // Validate the recipient email given whether it was being gifted or not. If it was a gift
  // then the e-mail was required, but if it was bought for self then we don't need the email.
  def validateRecipientEmailField(recipientChoice: RecipientChoice)
  : Either[FormError, Option[String]] =
  {
    recipientChoice match {
      case RecipientChoice.Self =>
        Right(None)

      case RecipientChoice.Other =>
        for (
          email <- check.isSomeValue(toValidate, "Required").right;
          _ <- check.isEmailAddress(email, "Invalid email address").right
        ) yield {
          Some(email)
        }
    }
  }

  def validateWrittenMessageChoice: Either[FormError, WrittenMessageChoice] = {
    for (
      param <- check.isSomeValue(toValidate, "Required").right;
      messageChoice <- check.isSomeValue(
                         WrittenMessageChoice(param),
                         "Message choice must be valid"
                       ).right
    ) yield {
      messageChoice
    }
  }

  def validateWrittenMessage(messageChoice: WrittenMessageChoice)
  : Either[FormError, Option[String]] =
  {
    import WrittenMessageChoice._

    messageChoice match {
      // There are no messages for signature only or for when the celebrity choosesn
      case CelebrityChoosesMessage | SignatureOnly =>
        Right(None)

      case SpecificMessage =>
        for (
          writtenMessage <- check.isSomeValue(toValidate, "Required").right;
          _ <- check.isBetweenInclusive(
                 minWrittenMessageChars,
                 maxWrittenMessageChars,
                 writtenMessage.length,
                 writtenMessageLengthErrorString
               ).right
        ) yield {
          Some(writtenMessage)
        }
    }
  }

  def validateNoteToCelebrity: Either[FormError, Option[String]] = {
    // If there was a string, make sure it's valid
    val maybeErrorOrValidNote = toValidate.headOption.map { note =>
      for (_ <- check.isBetweenInclusive(
                  minNoteToCelebChars,
                  maxNoteToCelebChars,
                  note.length,
                  noteToCelebLengthErrorString
                ).right
      ) yield {
        Some(note)
      }
    }

    // Return the validation or Right(None) [meaning that there was no string]
    maybeErrorOrValidNote.getOrElse(Right(None))
  }

  //
  // Private members
  //
}

object PurchaseFormValidations {
  private[purchase] val nameLengthErrorString = "Must be between two and 30 characters"

  private[purchase] val minWrittenMessageChars = 5
  private[purchase] val maxWrittenMessageChars = 140
  private[purchase] val minNoteToCelebChars = minWrittenMessageChars
  private[purchase] val maxNoteToCelebChars = maxWrittenMessageChars

  private[purchase] val writtenMessageLengthErrorString = {
    textAreaLengthErrorString(minWrittenMessageChars, maxWrittenMessageChars)
  }

  private[purchase] val noteToCelebLengthErrorString = {
    textAreaLengthErrorString(minNoteToCelebChars, maxNoteToCelebChars)
  }

  def textAreaLengthErrorString(min: Int, max: Int): String = {
    "Must be between " + min + " and " + max + " characters"
  }
}