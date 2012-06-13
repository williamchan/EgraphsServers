package services.social

import com.google.inject.{Provider, Inject}
import controllers.website.GetFacebookLoginCallbackEndpoint
import java.util.Properties
import play.libs.WS
import services.http.PlayConfig
import sjson.json.Serializer

/**
 * Provides our Facebook App ID to Guice as an injectable string
 *
 * Usage:
 * {{{
 *   class MyClassThatUsesFacebook @Inject() (@FacebookAppId fbAppId: String) {
 *     // Do something with the string in here
 *   }
 * }}}
 */
private[social] class FacebookAppIdProvider @Inject()(@PlayConfig playConfig: Properties) extends Provider[String] {
  def get(): String = {
    playConfig.getProperty("fb.appid")
  }
}

object Facebook {

  // Facebook user properties
  lazy val _id = "id"
  lazy val _name = "name"
  lazy val _email = "email"

  // CSRF guard key
  lazy val _fbState = "fbState"

  /**
   * Facebook documentation: https://developers.facebook.com/docs/authentication/server-side/
   * @param fbAppId see Facebook documentation
   * @param state see Facebook documentation
   * @return Facebook url at which to begin the Oauth flow
   */
  def getFbOauthUrl(fbAppId: String, state: String): String = {
    val fbCallbackUrl = GetFacebookLoginCallbackEndpoint.getCallbackUrl
    "https://www.facebook.com/dialog/oauth?client_id=" + fbAppId +
      "&redirect_uri=" + fbCallbackUrl +
      "&scope=email" +
      "&state=" + state
  }

  /**
   * Facebook documentation: https://developers.facebook.com/docs/authentication/server-side/
   * @param code see Facebook documentation
   * @param facebookAppId see Facebook documentation
   * @param fbAppSecret see Facebook documentation
   * @return an access token, which can be used to make calls to Facebook's API on behalf of a Facebook user
   */
  def getFbAccessToken(code: String, facebookAppId: String, fbAppSecret: String): String = {
    val fbCallbackUrl = GetFacebookLoginCallbackEndpoint.getCallbackUrl
    val fbOauthTokenUrlStr = "https://graph.facebook.com/oauth/access_token?" +
      "client_id=" + facebookAppId +
      "&redirect_uri=" + fbCallbackUrl +
      "&client_secret=" + fbAppSecret +
      "&code=" + code

    val response = WS.url(fbOauthTokenUrlStr).get()
    response.getStatus.intValue() match {
      case play.mvc.OK => {
        // expected String format is: "access_token=USER_ACESS_TOKEN&expires=NUMBER_OF_SECONDS_UNTIL_TOKEN_EXPIRES"
        val responseStr = response.getString
        val prefix = "access_token="
        responseStr.substring(responseStr.indexOf(prefix) + prefix.length, responseStr.indexOf("&"))

      }
      case _ => throw new RuntimeException("Facebook authentication error encountered: " + response.getString)
    }
  }

  /**
   * @param accessToken access token unique for a Facebook user and obtained from Facebook via getFbAccessToken
   * @return map of Facebook user data including id, name, and email
   */
  def getFbUserInfo(accessToken: String): Map[String, AnyRef] = {
    val fields = List(_id, _name, _email).mkString(",")
    val fbUserInfoUrlStr = "https://graph.facebook.com/me?fields=" + fields + "&access_token=" + accessToken
    val response = WS.url(fbUserInfoUrlStr).get()
    response.getStatus.intValue() match {
      case play.mvc.OK => {
        Serializer.SJSON.in[Map[String, AnyRef]](response.getString)
      }
      case _ => throw new RuntimeException("Facebook access error encountered: " + response.getString)
    }
  }
}
