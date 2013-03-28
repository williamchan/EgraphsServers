package models.frontend.website

/**
 * ViewModel for rendering the recover account form.
 *
 * See [[views.html.frontend.account_recover]]
 *
 * @param email the email as submitted
 */
case class RecoverAccountViewModel(
  email: String
)