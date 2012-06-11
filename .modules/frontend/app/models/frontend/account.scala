package models.frontend.account

/**
 * The settings as seen on the account_settings page
 */
case class AccountSettings(
  name:String,
  username:String,
  email:String,
  passwordLength:Int,
  galleryUrl:String
)



