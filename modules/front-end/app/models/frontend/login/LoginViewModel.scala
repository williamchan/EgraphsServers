package models.frontend.login

/**
 * ViewModel for rendering the existing customer form on the login page.
 *
 * See [[views.html.frontend.login]]
 *
 * @param loginEmail the email address as submitted
 * @param loginPassword the password as submitted
 */
case class LoginViewModel(
  loginEmail: String,
  loginPassword: String
)