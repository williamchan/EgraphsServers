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
  addressLine1: Field[String],
  addressLine2: Field[String],
  city: Field[String],
  state: Field[String],
  postalCode: Field[String],
  notice_stars: Field[String],
  generalErrors: Iterable[FormError]
)

case class AccountVerificationForm(
  newPassword: Field[String],
  passwordConfirm: Field[String],
  email: Field[String],
  secretKey: Field[String]
)
