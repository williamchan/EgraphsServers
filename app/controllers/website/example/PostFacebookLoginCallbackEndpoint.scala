package controllers.website.example

import play.mvc.Controller
import services.http.ControllerMethod
import play.libs.Codec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.Predef._
import util.parsing.json.JSON

private[controllers] trait PostFacebookLoginCallbackEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  /**
   * Documentation at https://developers.facebook.com/docs/authentication/signed_request/
   */
  def postFacebookLoginCallback = controllerMethod() {
    val signed_request = params.get("signed_request")
    signed_request.split('.') match {
      case Array(signatureEncoded, dataEncoded) => {
        val dataStr = new String(Codec.decodeBASE64(dataEncoded))
        val data = JSON.parseFull(dataStr).get.asInstanceOf[Map[String, Any]]

        // Facebook params
        val algorithm = data.get("algorithm").getOrElse("")
        val expires = data.get("expires").getOrElse(0)
        val issued_at = data.get("issued_at").getOrElse(0)
        val oauth_token = data.get("oauth_token").getOrElse("")
        val user_id = data.get("user_id").getOrElse("")
        val registrationData = data.get("registration").get.asInstanceOf[Map[String, Any]]
        val registrationName = registrationData.get("name").getOrElse("")
        val registrationEmail = registrationData.get("email").getOrElse("")

        // check algorithm
        if (algorithm.toString.toUpperCase != "HMAC-SHA256") {
          throw new RuntimeException("Facebook authentication algorithm was not HMAC-SHA256")
        }

        // check signature
        val expected_sig = getHash(dataEncoded)
        // THIS IS DIFFERENT FROM FACEBOOK EXAMPLE CODE!!!
        if (expected_sig != replaceUrlChars(signatureEncoded)) {
          throw new RuntimeException("Facebook authentication signature was incorrect")
        }

        println("algorithm " + algorithm)
        println("expires " + expires)
        println("issued_at " + issued_at)
        println("oauth_token " + oauth_token)
        println("user_id " + user_id)
        println("registrationName " + registrationName)
        println("registrationEmail " + registrationEmail)

        dataStr
      }
      case _ => error("smthg wrong with facebook redirect")
    }
  }

  def getHash(baseString: String) = {
    val secretStr = "5c7355374b6a1c7847e75a32b6d5f08a"
    val keyBytes = secretStr.getBytes("UTF-8")
    val mac = Mac.getInstance("HMACSHA256")
    val secretKey = new SecretKeySpec(keyBytes, mac.getAlgorithm)
    mac.init(secretKey)
    val text = baseString.getBytes("UTF-8")
    val encodedText = mac.doFinal(text)
    new String(Codec.encodeBASE64(encodedText)).replace("=", "")
  }

  def replaceUrlChars(str: String): String = {
    str.replace('-', '+').replace('_', '/')
  }

  def base64_url_decode(encoded: String): String = {
    new String(Codec.decodeBASE64(replaceUrlChars(encoded)))
  }
}