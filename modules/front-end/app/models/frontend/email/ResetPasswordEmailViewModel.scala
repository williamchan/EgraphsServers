package models.frontend.email

case class ResetPasswordEmailViewModel(
  email: String,
  resetPasswordUrl: String
)