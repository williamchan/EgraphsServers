package models.frontend.header

/**
 * Trait that represents the data about the logged in user that is necessary to
 * render the page header.
 **/
sealed trait HeaderLoggedInStatus


/**
 * Implementation for users that are logged in.
 *
 * @param name the user's name. If the name isn't available then we can default this
 *     to the username
 * @param profileUrl link to the user's profile
 * @param accountSettingsUrl link to the user's account settings page.
 * @param galleryUrl link to the user's gallery.
 * @param logoutUrl link to log out the user.
 */
case class HeaderLoggedIn (
  name: String,
  profileUrl: String,
  accountSettingsUrl: String,
  galleryUrl: String,
  logoutUrl: String
) extends HeaderLoggedInStatus


/**
 * Implementation for users that are not logged in
 *
 * @param loginUrl URL to the login_page page.
 */
case class HeaderNotLoggedIn (
  loginUrl: String
) extends HeaderLoggedInStatus
