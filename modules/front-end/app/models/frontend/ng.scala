package models.frontend

import play.api.templates.Html

/**
 * Helpers for reducing boilerplate when writing angular forms.
 *
 * @param form the name of the form. e.g. "stripeToken". The actual HTML form wrapping this input should
 *             be called "stripeTokenForm" in this case.
 * @param name the name of the input. e.g. "recipientName"
 */
case class NgField(form: String, name: String) {
  /** Makes a name for the control out of the parent's form name */
  def control: Html = Html(form + "Form." + name)

  /** Angular logic for whether this field is both altered and invalid */
  def dirtyAndInvalid: Html = control += Html(".$dirty && ") += control += Html(".$invalid")

  /** an id composed of the form and input name */
  def id: Html = Html(form) += Html("-") += Html(name)

  /** A default ng-model attribute for angular */
  def ngModel: Html = Html(s"$form.$name")

  /** Dumps in id, name, and ng-model as a series of html tag attributes */
  def identityAttributes: Html = Html("id='") += id += Html("' name='") += Html(name) += Html("' ng-model='") += ngModel += Html("'")

  /** Creates default error divs for different angular error cases that can happen to the NgController */
  def ifError(errors: String*)(body: => Html): Html = {
    val errorConditionals = errors.map(err => control + ".$error." + err).mkString(" || ")
    Html("<div class='field-error' ng-show='" + errorConditionals + "'>" + body.body + "</div>")
  }

  /** Returns the angular expression representing that the user has interacted with the input */
  def userHasAttended: Html = {
    control += Html(".userAttention.attended")
  }

  /** Returns the angular expression representing that the input is in an error state */
  def invalid: Html = {
    control += Html(".$invalid")
  }

  /** Returns the angular expression representing that the input is currently submitting in an ajax form */
  def submitting: Html = {
    control += Html(".$submitting")
  }

  def invalidAndUserHasAttendedAndNotSubmitting: Html = {
    userHasAttended += Html(" && ") += invalid += Html(" && !") += submitting
  }

  /**
   * Produces a standard error div with visibility governed by whether this form control
   * has errors or not, and is in a state where it is appropriate to show those errors
   * to the user.
   **/
  def errorDiv(body: => Html): Html = {
    Html("<div class=\"errors\" ng-show=\"") += invalidAndUserHasAttendedAndNotSubmitting += Html("\">") += body += Html("</div>")
  }

  /** A standard set of errors as-per our API spec. */
  def standardErrors: Html = {
    ifError("required", "remote_required") {Html("required")} +=
    ifError("email") {Html("invalid e-mail")} +=
    ifError("remote_invalid_length") {Html("invalid length")} +=
    ifError("remote_invalid_format", "remote_unexpected_type") {Html("invalid format")}
  }

  def standardErrorDiv: Html = {
    errorDiv(standardErrors)
  }
}

object NgField {
  def withField(form: String, name: String)(body: NgField => Html): Html = {
    body(NgField(form, name))
  }
}
