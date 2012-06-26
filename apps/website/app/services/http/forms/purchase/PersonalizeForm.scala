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
  val recipientChoice = field(Params.IsGift).validatedBy { paramValues =>
    checkField(paramValues).isRecipientChoice
  }

  val recipientName = field(Params.RecipientName).validatedBy { paramValues =>
    checkField(paramValues).isName
  }

  val recipientEmail = field(Params.RecipientEmail).validatedBy { paramValues =>
    for (
      validRecipientChoice <- check.dependentFieldIsValid(recipientChoice).right;
      validEmailOption <- checkField(paramValues)
                            .isRecipientEmailGivenRecipient(validRecipientChoice).right
    ) yield {
      validEmailOption
    }
  }

  val writtenMessageRequest = field(Params.WrittenMessageRequest).validatedBy { paramValues =>
    checkField(paramValues).isWrittenMessageRequest
  }


  // Necessary only if writtenMessageRequest was specified, then it must be no more than 140 characters
  val writtenMessageRequestText = field(Params.WrittenMessageRequestText).validatedBy { paramValues =>
    for (
      messageRequest <- check.dependentFieldIsValid(writtenMessageRequest).right;
      validMessageTextOption <- checkField(paramValues)
                                  .isWrittenMessageTextGivenRequest(messageRequest).right
    ) yield {
      validMessageTextOption
    }
  }

  val noteToCelebrity = field(Params.NoteToCelebrity).validatedBy { paramValues =>
    checkField(paramValues).isOptionalNoteToCelebrity
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
      noteToCelebrity.value.get
    )
  }
}


object PersonalizeForm {
  object Params {
    val IsGift = "order.personalize.isGift"
    val RecipientName = "order.personalize.recipient.name"
    val RecipientEmail = "order.personalize.recipient.email"
    val WrittenMessageRequest = "order.personalize.message.choice"
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
    writtenMessageMaybe: Option[String],
    noteToCelebriity: Option[String]
  )
}


class PersonalizeFormFactory @Inject()(formChecks: FormChecks, purchaseFormChecks: PurchaseFormChecksFactory)
  extends ReadsForm[PersonalizeForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): PersonalizeForm = {
    new PersonalizeForm(readable, formChecks, purchaseFormChecks)
  }

  //
  // ReadsForm[PersonalizeForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): PersonalizeForm = {
    apply(readable)
  }
}



