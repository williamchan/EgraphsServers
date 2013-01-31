package models.frontend

import play.api.templates.Html

case class NgField(form: String, name: String) {
  val control = form + "Form." + name
  def dirtyAndInvalid = control + ".$dirty && " + control + ".$invalid"
  def id = form + "-" + name
  def ngModel = form + "." + name
  def identityAttributes = "id='" + id + "' name='" + name + "' ng-model='" + ngModel + "'"

  def ifError(errors: String*)(body: => Html): Html = {
    val errorConditionals = errors.map(err => control + ".$error." + err).mkString(" || ")
    Html("<div class='field-error' ng-show='" + errorConditionals + "'>" + body.body + "</div>")
  }

  def userHasAttended = {
    control + ".userAttention.attended"
  }

  def invalid = {
    control + ".$invalid"
  }

  def invalidAndUserHasAttended = {
    userHasAttended + " && " + invalid
  }

  def errorDiv(body: => Html): Html = {
    Html("<div class=\"errors\" ng-show=\"" + invalidAndUserHasAttended + "\">" + body + "</div>")
  }

  def standardErrors: Html = {
    ifError("required", "remote_required") {Html("required")} +
    ifError("email") {Html("invalid e-mail")} +
    ifError("remote_invalid_length") {Html("invalid length")} +
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