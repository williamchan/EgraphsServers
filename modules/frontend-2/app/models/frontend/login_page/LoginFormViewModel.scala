package models.frontend.login_page

import models.frontend.forms.{FormError, Field}

/**
 * The log-in form as seen on the login page.
 *
 * See [[views.html.frontend.login]]
 *
 * @param email the submitted email address
 * @param password the submitted password
 * @param generalErrors errors with the form that were not associated
 *                      with any particular field (e.g. bad password)
 * @param actionUrl the url to which the user should POST login info
 * @param forgotPasswordUrl the URL that the browser should access
 *     to begin a password recovery flow.
 **/
case class LoginFormViewModel(
  email: Field[String],
  password: Field[String],
  generalErrors: Iterable[FormError],
  actionUrl: String,
  forgotPasswordUrl: String
)