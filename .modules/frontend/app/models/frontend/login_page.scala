package models.frontend.login_page

import models.frontend.forms.{Field, FormError}

case class LoginForm(
  email: Field[String],
  password: Field[String],
  generalErrors:Iterable[FormError]
)