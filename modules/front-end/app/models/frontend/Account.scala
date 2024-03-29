package models.frontend.account

import models.frontend.forms.{Field, FormError}

/**
 * The settings as seen on the account_settings page
 */
case class AccountSettingsForm(
  fullname: Field[String],
  username: Field[String],
  email: Field[String],
  galleryVisibility: Field[String],
  oldPassword: Field[String],
  newPassword: Field[String],
  passwordConfirm: Field[String],
  notice_stars: Field[String],
  generalErrors: Iterable[FormError]
)

case class AccountPasswordResetForm(
  newPassword: Field[String],
  passwordConfirm: Field[String],
  email: Field[String],
  secretKey: Field[String]
)

case class AccountRecoverForm(
  email: Field[String]
)
