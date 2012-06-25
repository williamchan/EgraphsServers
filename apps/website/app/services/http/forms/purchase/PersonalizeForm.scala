package services.http.forms.purchase

import com.google.inject.Inject
import services.http.forms.{FormError, ReadsForm, FormChecks, Form}
import models.enums.{WrittenMessageChoice, RecipientChoice}


class PersonalizeForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[PersonalizeForm.Validated]
{
  import PersonalizeForm.Params

  private def validations(toValidate: Iterable[String]) = {
    new PurchaseFormValidation(toValidate, check)
  }

  //
  // Field values and validations
  //
  val recipientChoice = new Field[RecipientChoice]() {
    val name = Params.IsGift

    def validate: Either[FormError, RecipientChoice] = {
      validations(stringsToValidate).validateRecipientChoiceField
    }
  }

  val recipientName = new Field[String] {
    val name = Params.RecipientName

    def validate: Either[FormError, String] = {
      validations(stringsToValidate).validateRecipientNameField
    }
  }

  val recipientEmail = new Field[Option[String]] {
    val name = Params.RecipientEmail

    def validate: Either[FormError, Option[String]] = {
      for (
        validRecipientChoice <- check.dependentFieldIsValid(recipientChoice).right;
        validEmailOption <- validations(stringsToValidate)
                              .validateRecipientEmailField(validRecipientChoice).right
      )
      yield {
        validEmailOption
      }
    }
  }

  val writtenMessageChoice = new Field[WrittenMessageChoice] {
    val name = Params.WrittenMessageChoice

    def validate = {
      validations(stringsToValidate).validateWrittenMessageChoice
    }
  }

  // Necessary only if writtenMessageChoice was specified, then it must be no more than 140 characters
  val writtenMessage = new Field[Option[String]] {
    val name = Params.WrittenMessage

    def validate: Either[FormError, Option[String]] = {
      for (
        validMessageChoice <- check.dependentFieldIsValid(writtenMessageChoice).right;
        validMessageTextOption <- validations(stringsToValidate)
                                    .validateWrittenMessage(validMessageChoice).right)
      yield {
        validMessageTextOption
      }
    }
  }

  val noteToCelebrity = new Field[Option[String]] {
    val name = Params.NoteToCelebrity

    def validate: Either[FormError, Option[String]] = {
      validations(stringsToValidate).validateNoteToCelebrity
    }
  }

  //
  // Form[ValidatedPersonalizeForm] members
  //
  protected def formAssumingValid: PersonalizeForm.Validated = {
    PersonalizeForm.Validated()
  }
}


object PersonalizeForm {
  object Params {
    val IsGift = "order.personalize.isGift"
    val RecipientName = "order.personalize.recipient.name"
    val RecipientEmail = "order.personalize.recipient.email"
    val WrittenMessageChoice = "order.personalize.message.choice"
    val WrittenMessage = "order.personalize.message.text"
    val NoteToCelebrity = "order.personalize.note_to_celeb"
  }

  val minMessageChars = 5
  val maxMessageChars = 140

  val messageLengthWarning = "Should be between " +
    minMessageChars + " and " + maxMessageChars + " characters."

  case class Validated()
}


class PersonalizeFormFactory @Inject()(formChecks: FormChecks)
  extends ReadsForm[PersonalizeForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): PersonalizeForm = {
    new PersonalizeForm(readable, formChecks)
  }

  //
  // ReadsForm[PersonalizeForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): PersonalizeForm = {
    apply(readable)
  }
}



