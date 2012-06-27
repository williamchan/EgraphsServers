package models.frontend.header

sealed trait HeaderLoggedInStatus


case class HeaderLoggedIn (
  name: String,
  profileUrl: String,
  accountSettingsUrl: String,
  galleryUrl: String,
  logoutUrl: String
) extends HeaderLoggedInStatus


case class HeaderNotLoggedIn (
  loginUrl: String
) extends HeaderLoggedInStatus
