package models.frontend.login_page

import models.frontend.forms.{Field, FormError}

/**
 * The log-in form as seen on the login page.
 *
 * See login.scala.html
 *
 * @param email the submitted email address
 * @param password the submitted password
 * @param generalErrors errors with the form that were not associated
 *     with any particular field (e.g. bad password)
 **/
case class LoginForm(
  email: Field[String],
  password: Field[String],
  generalErrors:Iterable[FormError]
)