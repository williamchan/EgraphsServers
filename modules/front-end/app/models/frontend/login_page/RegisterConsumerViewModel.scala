package models.frontend.login_page

import models.frontend.forms.{FormError, Field}

/**
 * ViewModel for rendering the account registration form on the login page.
 *
 * See [[views.html.frontend.login]]
 *
 * @param email the email address as submitted
 * @param password the password as submitted
 * @param bulkEmail true if the consumer is signing up for bulk email.
 */
case class RegisterConsumerViewModel(
  email: String,
  password: String,
  bulkEmail: Boolean
)