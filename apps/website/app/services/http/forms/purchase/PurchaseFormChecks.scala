package services.http.forms.purchase

import services.http.forms.{FormChecks, FormError}
import models.enums.{WrittenMessageRequest, RecipientChoice, PrintingOption}
import com.google.inject.Inject

/**
 * Checks used to perform field validation specifically on parameters submitted for the
 * purchase form.
 *
 * Prefer to access instances via the [[services.http.forms.purchase.PurchaseFormChecksFactory]].
 *
 * See the [[services.http.forms.purchase.PersonalizeForm]] for good usage examples.
 *
 * @param toValidate the string to validate
 * @param check the generic form checking library
 */
class PurchaseFormChecks(toValidate: Iterable[String], check: FormChecks) {
  import PurchaseFormChecks._

  /**
   * True on the right that the provided checkbox form parameter
   * specified a printing option.
   */
  def isPrintingOption: Either[FormError, PrintingOption] = {
    import PrintingOption.{HighQualityPrint, DoNotPrint}

    for (
      doPrint <- check.isChecked(toValidate.headOption).right
    ) yield {
      if (doPrint) HighQualityPrint else DoNotPrint
    }
  }

  /**
   * True on the right if the provided parameter was a valid one for a
   * checkbox. This makes it succeed if it didn't exist (false) or if it had
   * some expected affirmative otherwise ("yes", "1", etc)
   */
  def isCheckBoxValue: Either[FormError, Boolean] = {
    for (
      checked <- check.isChecked(toValidate.headOption).right
    ) yield {
      checked
    }
  }

  /**
   * Returns a [[models.Product]] on the right if the provided parameter
   * mapped to a valid product ID. It does not check the product's inventory batch
   * for availability.
   */
  def isProductId: Either[FormError, models.Product] = {
    for (
      param <- check.isSomeValue(toValidate, requiredError).right;
      paramAsLong <- check.isLong(param).right;
      product <- check.isProductId(paramAsLong).right
    ) yield {
      product
    }
  }

  /**
   * True that the checkbox form parameter specified a recipient choice
   */
  def isGift
  : Either[FormError, RecipientChoice] =
  {
    for (
      isGift <- check.isChecked(toValidate.headOption).right
    ) yield {
      if (isGift) RecipientChoice.Other else RecipientChoice.Self
    }
  }

  /**
   * Returns None on the right if this is a self-purchase (because we will get the e-mail address later)
   * and Some(email) on the right if it was both a gift and a valid e-mail address.
   *
   * @param recipientChoice the target for the egraph: self or someone else.
   */
  def isRecipientEmailGivenRecipient(recipientChoice: RecipientChoice)
  : Either[FormError, Option[String]] =
  {
    recipientChoice match {
      case RecipientChoice.Self =>
        Right(None)

      case RecipientChoice.Other =>
        isEmail.right.map(email => Some(email))
    }
  }

  /**
   * Returns a [[models.enums.WrittenMessageRequest]] on the right if the provided parameters
   * mapped to a valid enum value.
   */
  def isWrittenMessageRequest: Either[FormError, WrittenMessageRequest] = {
    for (
      param <- check.isSomeValue(toValidate, requiredError).right;
      messageRequest <- check.isSomeValue(
                         WrittenMessageRequest(param),
                         "Message choice must be valid"
                       ).right
    ) yield {
      messageRequest
    }
  }

  /**
   * Returns None on the right if the purchaser didn't want to specify a message. Otherwise
   * returns Some(valid message) on the right.
   *
   * @param messageRequest choice for written message: signature only, chosen-by-celebrity,
   *    or a specific message
   */
  def isWrittenMessageTextGivenRequest(messageRequest: WrittenMessageRequest)
  : Either[FormError, Option[String]] =
  {
    import WrittenMessageRequest._

    messageRequest match {
      // There are no messages for signature only or for when the celebrity choosesn
      case CelebrityChoosesMessage | SignatureOnly =>
        Right(None)

      case SpecificMessage =>
        for (
          writtenMessage <- check.isSomeValue(toValidate, requiredError).right;
          messageOfCorrectLength <- check.isBetweenInclusive(
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

  /**
   * Returns Some(note to the celebrity) on the right if the string existed, but if it wasn't
   * provided it returns None.
   */
  def isOptionalNoteToCelebrity: Either[FormError, Option[String]] = {
    // If there was a string, make sure it's valid
    val maybeNote = toValidate.headOption.filter(note => note != "")
    val maybeErrorOrValidNote = maybeNote.map { note =>
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

  /**
   * Return the payment token on the right if it existed. Later on we could add
   * an actual call to the Stripe API to make sure the token is legit.
   */
  def isPaymentToken: Either[FormError, String] = {
    for (param <- check.isSomeValue(toValidate, requiredError).right) yield {
      param
    }
  }

  /**
   * True on the right if the form parameter was a valid zip code. We could make this actually
   * check zip codes later on if we wanted.
   */
  def isZipCode: Either[FormError, String] = {
    for (
      param <- check.isSomeValue(toValidate, requiredError).right;
      _ <- check.isBetweenInclusive(
                          5,
                          7,
                          param.length,
                          "Not a valid postal code")
                        .right
//      SER-196: Rewrite to be a general solution for international addresses
//      _ <- check.isInt(param, "Not a valid postal code").right
    ) yield {
      param
    }
  }

  /**
   * Returns a human name on the right if it existed and passed length checks.
   */
  def isName: Either[FormError, String] = {
    for (
      name <- check.isSomeValue(toValidate, requiredError).right;
      _ <- check.isBetweenInclusive(2, 30, name.length, nameLengthErrorString).right
    ) yield {
      name
    }
  }

  /**
   * Returns an e-mail address on the right if it existed and passed regex checks.
   */
  def isEmail: Either[FormError, String] = {
    for (
      param <- check.isSomeValue(toValidate, requiredError).right;
      email <- check.isEmailAddress(param).right
    ) yield {
      email
    }
  }

  /**
   * Returns the set of strings on the right if there was at least one of them.
   */
  def isPresent: Either[FormError, Iterable[String]] = {
    check.isPresent(toValidate = toValidate)
  }

  //
  // Private members
  //
}

object PurchaseFormChecks {
  private[purchase] val requiredError = "Required field"
  private[purchase] val nameLengthErrorString = "Must be between 2 and 30 characters"

  /** The maximum number of characters a written message request can contain */
  val minWrittenMessageChars = 5
  val maxWrittenMessageChars = 60

  val minNoteToCelebChars = 5
  val maxNoteToCelebChars = 140

  private[purchase] val writtenMessageLengthErrorString = {
    textAreaLengthErrorString(minWrittenMessageChars, maxWrittenMessageChars)
  }

  private[purchase] val noteToCelebLengthErrorString = {
    textAreaLengthErrorString(minNoteToCelebChars, maxNoteToCelebChars)
  }

  private[purchase] def textAreaLengthErrorString(min: Int, max: Int): String = {
    "Must be between " + min + " and " + max + " characters"
  }
}

/**
 * Injectable accessor for PurchaseFormChecks.
 *
 * See the [[services.http.forms.purchase.PersonalizeForm]] for good usage examples.
 *
 * @param check low-level checkers used to perform these higher level checks.
 */
class PurchaseFormChecksFactory @Inject()(check: FormChecks){
  def apply(toValidate: Iterable[String]): PurchaseFormChecks = {
    new PurchaseFormChecks(toValidate, check)
  }
}