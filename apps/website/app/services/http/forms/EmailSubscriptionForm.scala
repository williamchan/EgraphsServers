package services.http.forms

/**
 * Define the fields (and validations thereof) for email subscription to a mailing list.
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *                  by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 *
 */

class EmailSubscriptionForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[EmailSubscriptionForm.Valid] {
  import EmailSubscriptionForm.Params

  //
  // Field values and validations
  //

  val email = field(Params.Email).validatedBy { toValidate =>
    for (
      submission <- check.isSomeValue(toValidate, "We're gonna need this").right;
      emailAddress <- check.isEmailAddress(submission, "Not an e-mail address.").right
    ) yield {
      emailAddress
    }
  }

  val listId = field(Params.ListId).validatedBy { toValidate =>
    for ( submission <- check.isSomeValue(toValidate, "We're gonna need this").right;
          validId <- check.isAlphaNumeric(submission, "Not a valid list id").right
    )
    yield {
      validId
    }
  }
  //
  // Form members
  //
  protected def formAssumingValid: EmailSubscriptionForm.Valid = {
    EmailSubscriptionForm.Valid(
      listId.value.get,
      email.value.get
    )

  }

}

object EmailSubscriptionForm {
  object Params {
    val Email = "email"
    val ListId = "listId"
  }

  /**
   * Fully validated version of the EmailSubscriptionForm
   * @param listId the id of the mailing list the user is signing up for
   * @param email the submitted email address for subscription to the given list.
   */
  case class Valid(listId: String, email: String)
}
