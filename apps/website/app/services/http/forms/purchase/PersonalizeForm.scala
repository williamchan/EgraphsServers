package services.http.forms.purchase

import com.google.inject.Inject
import services.http.forms.{ReadsForm, FormChecks, Form}
import models.enums.{WrittenMessageRequest, RecipientChoice}


/**
 * Purchase flow form for egraph personalization
 */
class PersonalizeForm(
  val paramsMap: Form.Readable,
  check: FormChecks,
  checkField: PurchaseFormChecksFactory
)
  extends Form[PersonalizeForm.Validated]
{
  import PersonalizeForm.Params

  //
  // Field values and validations
  //
  /** Whether the egraph is for yourself or a friend */
  val recipientChoice = field(Params.IsGift).validatedBy { paramValues =>
    checkField(paramValues).isGift
  }

  /** Name of the recipient (whether self or friend) */
  val recipientName = field(Params.RecipientName).validatedBy { paramValues =>
    checkField(paramValues).isName
  }

  /**
   * Recipient's email; not necessary if it's self (we'll just grab the e-mail from the purchase form later)
   */
  val recipientEmail = field(Params.RecipientEmail).validatedBy { paramValues =>
    for (
      validRecipientChoice <- check.dependentFieldIsValid(recipientChoice).right;
      validEmailOption <- checkField(paramValues)
                            .isRecipientEmailGivenRecipient(validRecipientChoice).right
    ) yield {
      validEmailOption
    }
  }

  /**
   * Choice for the written message: should the celebrity write something in particular,
   * write whatever he wants, or just sign the damn thing?
   */
  val writtenMessageRequest = field(Params.WrittenMessageRequest).validatedBy { paramValues =>
    checkField(paramValues).isWrittenMessageRequest
  }

  /**
   * Text of the message to write. Necessary only if writtenMessageRequest was specified, then it must be no more
   * than 140 characters.
   **/
  val writtenMessageRequestText = field(Params.WrittenMessageRequestText).validatedBy { paramValues =>
    for (
      messageRequest <- check.dependentFieldIsValid(writtenMessageRequest).right;
      validMessageTextOption <- checkField(paramValues)
                                  .isWrittenMessageTextGivenRequest(messageRequest).right
    ) yield {
      validMessageTextOption
    }
  }

  /** Optional personal note to the celebrity. "I'm your biggest fan", etc. */
  val noteToCelebrity = field(Params.NoteToCelebrity).validatedBy { paramValues =>
    checkField(paramValues).isOptionalNoteToCelebrity
  }
  
  val coupon = field(Params.Coupon).validatedBy { paramValues =>
    checkField(paramValues).isOptionalValidCouponCode
  }

  //
  // Form[ValidatedPersonalizeForm] members
  //
  protected def formAssumingValid: PersonalizeForm.Validated = {
    PersonalizeForm.Validated(
      recipientChoice.value.get,
      recipientName.value.get,
      recipientEmail.value.get,
      writtenMessageRequest.value.get,
      writtenMessageRequestText.value.get,
      noteToCelebrity.value.get,
      coupon.value.get
    )
  }
}


object PersonalizeForm {
  object Params {
    val IsGift = "order.personalize.isGift"
    val RecipientName = "order.personalize.recipient.name"
    val RecipientEmail = "order.personalize.recipient.email"
    val WrittenMessageRequest = "order.personalize.message.choice"
    val Coupon = "order.personalize.coupon.text"
    val WrittenMessageRequestText = "order.personalize.message.text"
    val NoteToCelebrity = "order.personalize.note_to_celeb"
  }

  val minMessageChars = 5
  val maxMessageChars = 140

  val messageLengthWarning = "Should be between " +
    minMessageChars + " and " + maxMessageChars + " characters."

  case class Validated(
    recipient: RecipientChoice,
    recipientName: String,
    recipientEmail: Option[String],
    writtenMessageRequest: WrittenMessageRequest,
    writtenMessageText: Option[String],
    noteToCelebriity: Option[String],
    coupon: Option[models.Coupon]
  )
}
