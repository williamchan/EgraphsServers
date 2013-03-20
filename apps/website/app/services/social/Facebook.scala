package services.social

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Await
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import play.api.http.Status
import _root_.frontend.formatting.DateFormatting.Conversions._
import models.FulfilledOrder
import services.logging.Logging

object Facebook extends Logging {

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
   * @param fbCallbackUrl see Facebook documentation
   * @return Facebook url at which to begin the Oauth flow
   */
  def getFbOauthUrl(fbAppId: String, state: String, fbCallbackUrl: String)(implicit request: RequestHeader): String = {
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
   * @param fbCallbackUrl see Facebook documentation
   * @return an access token, which can be used to make calls to Facebook's API on behalf of a Facebook user
   */
  //TODO: Should refactor this to return a promise instead of blocking.
  def getFbAccessToken(code: String, facebookAppId: String, fbAppSecret: String, fbCallbackUrl: String)
  (implicit request: RequestHeader)
  : String = 
  {
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
          val indexOfPrefix = responseStr.indexOf(prefix)
          if (indexOfPrefix < 0) {
            error("Facebook access token expected but not found. Request body = " + responseStr)
          }
          responseStr.substring(indexOfPrefix + prefix.length, responseStr.indexOf("&"))

        case _ => throw new RuntimeException("Facebook authentication error encountered: " + response.body)
      }
    }

    promisedAccessToken onFailure {
      case exception => throw new RuntimeException(exception)
    }

    Await.result(promisedAccessToken, 20 seconds)
  }

  /**
   * @param accessToken access token unique for a Facebook user and obtained from Facebook via getFbAccessToken
   * @return map of Facebook user data including id, name, and email
   */
  def getFbUserInfo(accessToken: String): JsValue = {
    val fields = List(_id, _name, _email).mkString(",")
    val fbUserInfoUrlStr = "https://graph.facebook.com/me?fields=" + fields + "&access_token=" + accessToken
    val promisedResponse = WS.url(fbUserInfoUrlStr).get()
    val promisedJson = promisedResponse.map { response =>
      response.status match {
        case Status.OK => response.json
        case _ => throw new RuntimeException("Facebook access error encountered: " + response.body)
      }
    }

    promisedJson onFailure {
      case exception => throw new RuntimeException(exception)
    }

    Await.result(promisedJson, 20 seconds)
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
    val formattedSigningDate = egraph.getSignedAt.formatDayAsPlainLanguage("PST")
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
