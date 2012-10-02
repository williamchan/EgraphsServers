package services.social

import com.google.inject.{Provider, Inject}
import controllers.website.GetFacebookLoginCallbackEndpoint
import java.util.Properties
import play.api.libs.ws.WS
import play.api.mvc.RequestHeader
import services.http.PlayConfig
import sjson.json.Serializer
import models.FulfilledOrder
import java.text.SimpleDateFormat
import play.api.http.Status
import controllers.routes.WebsiteControllers.getFacebookLoginCallback

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
  def getFbOauthUrl(fbAppId: String, state: String)(implicit request: RequestHeader): String = {
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
  def getFbAccessToken(code: String, facebookAppId: String, fbAppSecret: String)
  (implicit request: RequestHeader)
  : String = 
  {
    val fbCallbackUrl = GetFacebookLoginCallbackEndpoint.getCallbackUrl
    val fbOauthTokenUrlStr = "https://graph.facebook.com/oauth/access_token?" +
      "client_id=" + facebookAppId +
      "&redirect_uri=" + fbCallbackUrl +
      "&client_secret=" + fbAppSecret +
      "&code=" + code

    val promisedResponse = WS.url(fbOauthTokenUrlStr).get()
    val promisedAccessToken = promisedResponse.map { response =>
      response.status match {
        case Status.OK =>
          // expected String format is: "access_token=USER_ACESS_TOKEN&expires=NUMBER_OF_SECONDS_UNTIL_TOKEN_EXPIRES"
          val responseStr = response.body
          val prefix = "access_token="
          responseStr.substring(responseStr.indexOf(prefix) + prefix.length, responseStr.indexOf("&"))

        case _ => throw new RuntimeException("Facebook authentication error encountered: " + response.body)
      }
    }

    promisedAccessToken.await.fold(requestError => throw requestError, accessToken => accessToken)
  }

  /**
   * @param accessToken access token unique for a Facebook user and obtained from Facebook via getFbAccessToken
   * @return map of Facebook user data including id, name, and email
   */
  def getFbUserInfo(accessToken: String): Map[String, AnyRef] = {
    val fields = List(_id, _name, _email).mkString(",")
    val fbUserInfoUrlStr = "https://graph.facebook.com/me?fields=" + fields + "&access_token=" + accessToken
    val promisedResponse = WS.url(fbUserInfoUrlStr).get()
    val promisedJsonMap = promisedResponse.map { response =>
      response.status match {
        case Status.OK => Serializer.SJSON.in[Map[String, AnyRef]](response.body)
        case _ => throw new RuntimeException("Facebook access error encountered: " + response.body)
      }
    }

    promisedJsonMap.await.fold(requestError => throw requestError, jsonMap => jsonMap)
  }

  /**
   * @param fulfilledOrder order and egraph
   * @param thumbnailUrl url to thumbnail image
   * @param viewEgraphUrl url to egraph page
   * @return a link with everything Facebook needs to make a wall post
   */
  def getEgraphShareLink(fbAppId: String, fulfilledOrder: FulfilledOrder, thumbnailUrl: String, viewEgraphUrl: String): String = {
    val order = fulfilledOrder.order
    val egraph = fulfilledOrder.egraph
    val celebName = order.product.celebrity.publicName
    val formattedSigningDate = new SimpleDateFormat("MMMM dd, yyyy").format(egraph.getSignedAt)
    views.frontend.Utils.getFacebookShareLink(
      appId = fbAppId,
      picUrl = thumbnailUrl,
      name = celebName + " egraph for " + order.recipientName,
      caption = "Created by " + celebName + " on " + formattedSigningDate,
      description = "",
      link = viewEgraphUrl
    )
  }
}
