package models.frontend.login_page

import models.frontend.forms.{FormError, Field}

/**
 * ViewModel for rendering the account registration form on the login page.
 *
 * See [[views.frontend.html.login]]
 *
 * @param email the email address as submitted
 * @param password the password as submitted
 * @param generalErrors any errors that weren't directly associated with either field
 * @param actionUrl the URL to which the user's browser should post registration
 *     information.
 */
case class AccountRegistrationFormViewModel(
  email: Field[String],
  password: Field[String],
  generalErrors: Iterable[FormError],
  actionUrl: String
) {
  /** Returns true that the view model has at least one error */
  def hasErrors: Boolean = {
    email.error.isDefined || password.error.isDefined || !generalErrors.isEmpty
  }
}
