package models.frontend.login_page

/**
 * ViewModel for rendering the account registration form on the login page.
 *
 * See [[views.html.frontend.login]]
 *
 * @param registerEmail the email address as submitted
 * @param registerPassword the password as submitted
 * @param bulkEmail true if the consumer is signing up for bulk email.
 */
case class RegisterConsumerViewModel(
  registerEmail: String,
  registerPassword: String,
  bulkEmail: Boolean
)