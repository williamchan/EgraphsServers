package services.http.forms.purchase

import com.google.inject.Inject
import services.http.forms.{FormError, ReadsForm, FormChecks, Form}
import models.enums.{WrittenMessageChoice, RecipientChoice}
import models.enums.RecipientChoice.EnumVal


class PersonalizeForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[PersonalizeForm.Validated]
{
  import PersonalizeForm.Fields

  //
  // Field values and validations
  //
  val recipientChoice = new Field[RecipientChoice.EnumVal]() {
    val name = Fields.IsGift

    def validate: Either[FormError, RecipientChoice.EnumVal] = {
      for (isGift <- check.isChecked(stringsToValidate.headOption).right) yield {
        if (isGift) RecipientChoice.Other else RecipientChoice.Self
      }
    }
  }

  val recipientName = new RequiredField[String](Fields.RecipientName) {
    def validateIfPresent: Either[FormError, String] = {
      for (_ <- check.isAtLeast(2, stringToValidate.length, "Must be at least 2 characters").right)
      yield {
        stringToValidate
      }
    }
  }

  val recipientEmail = new Field[Option[String]] {
    val name = Fields.RecipientEmail

    def validate: Either[FormError, Option[String]] = {
      val emailOption = stringsToValidate.headOption

      for (validRecipientChoice <- check.dependentFieldIsValid(recipientChoice).right;
           validEmailOption <- validateEmailGivenRecipient(emailOption, validRecipientChoice).right)
      yield {
        validEmailOption
      }
    }

    // Validate the recipient email given whether it was being gifted or not. If it was a gift
    // then the e-mail was required, but if it was bought for self then we don't need the email.
    private def validateEmailGivenRecipient(email: Option[String], recipientChoice: RecipientChoice.EnumVal)
    : Either[FormError, Option[String]] =
    {
      recipientChoice match {
        case RecipientChoice.Self =>
          Right(None)

        case RecipientChoice.Other =>
          for (value <- check.isSomeValue(email).right;
               _ <- check.isEmailAddress(value, "Valid e-mail address required").right)
          yield {
            Some(value)
          }
      }
    }
  }

  val writtenMessageChoice = new RequiredField[WrittenMessageChoice.EnumVal](Fields.WrittenMessageChoice) {
    def validateIfPresent: Either[FormError, WrittenMessageChoice.EnumVal] = {
      for (messageChoice <- check.isSomeValue(
                              WrittenMessageChoice(stringToValidate),
                              "Message choice must be valid"
                            ).right)
      yield {
        messageChoice
      }
    }
  }

  // Necessary only if writtenMessageChoice was specified, then it must be no more than 140 characters
  val writtenMessage = new Field[Option[String]] {
    val name = Fields.WrittenMessage

    def validate: Either[FormError, Option[String]] = {
      val messageTextOption = stringsToValidate.headOption

      for (validMessageChoice <- check.dependentFieldIsValid(writtenMessageChoice).right;
           validMessageTextOption <- validateWrittenMessageGivenMessageChoice(
                                       messageTextOption,
                                       validMessageChoice
                                     ).right)
      yield {
        validMessageTextOption
      }
    }

    private def validateWrittenMessageGivenMessageChoice(
      writtenMessageOption: Option[String],
      messageChoice: WrittenMessageChoice.EnumVal
    ): Either[FormError, Option[String]] = {
      import WrittenMessageChoice._

      messageChoice match {
        case CelebrityChoosesMessage | SignatureOnly =>
          Right(None)

        case SpecificMessage =>
          for (writtenMessage <- check.isSomeValue(
                                   writtenMessageOption,
                                   "Required if you want a specific message written"
                                 ).right;
               messageChoice <- validateMessageOrNoteLength(writtenMessage).right)
          yield {
            Some(writtenMessage)
          }
      }
    }
  }

  val noteToCelebrity = new OptionalField[String](Fields.NoteToCelebrity) {
    def validateIfPresent: Either[FormError, String] = {
      validateMessageOrNoteLength(stringToValidate)
    }
  }
  //
  // Form[ValidatedPersonalizeForm] members
  //
  protected def formAssumingValid: PersonalizeForm.Validated = {
    PersonalizeForm.Validated()
  }

  //
  // Private members
  //
  private def validateMessageOrNoteLength(messageOrNote: String): Either[FormError, String] = {
    import PersonalizeForm.{minMessageChars, maxMessageChars, messageLengthWarning}
    for (_ <- check.isAtLeast(minMessageChars, messageOrNote.length, messageLengthWarning).right;
         _ <- check.isAtMost(maxMessageChars, messageOrNote.length, messageLengthWarning).right)
    yield {
      messageOrNote
    }
  }
}


object PersonalizeForm {
  object Fields {
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



