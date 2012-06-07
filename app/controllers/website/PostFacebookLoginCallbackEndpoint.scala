package controllers.website

import play.mvc.Controller
import play.libs.Codec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.Predef._
import util.parsing.json.JSON
import services.http.ControllerMethod
import play.Play
import controllers.WebsiteControllers
import play.mvc.results.Redirect
import services.Utils
import models.{Account, Customer, CustomerStore, AccountStore}
import services.db.{TransactionSerializable, DBSession}

/**
 * As the final step of the Facebook Oauth2 flow, Facebook posts against this controller to notify Egraphs that they
 * have authenticated the user. This controller then registers and/or logs in the user.
 */
private[controllers] trait PostFacebookLoginCallbackEndpoint {  this: Controller =>

  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  private def fbAppSecretKey = "fb.appsecret"

  /**
   * Documentation at https://developers.facebook.com/docs/authentication/signed_request/
   */
  def postFacebookLoginCallback = controllerMethod(openDatabase = false) {
    val signed_request = params.get("signed_request")
    signed_request.split('.') match {
      case Array(signatureEncoded, dataEncoded) => {
        val dataStr = new String(Codec.decodeBASE64(dataEncoded))
        val data = JSON.parseFull(dataStr).get.asInstanceOf[Map[String, Any]]

        val algorithm = data.get("algorithm").getOrElse("")
        val user_id = data.get("user_id").getOrElse("")
        val registrationData = data.get("registration").get.asInstanceOf[Map[String, Any]]
        val registrationName = registrationData.get("name").getOrElse("")
        val registrationEmail = registrationData.get("email").getOrElse("")
        // These other values are here if we are interested in them later.
        //        val expires = data.get("expires").getOrElse(0)
        //        val issued_at = data.get("issued_at").getOrElse(0)
        //        val oauth_token = data.get("oauth_token").getOrElse("")

        validateFacebookCallback(algorithm.toString, dataEncoded, signatureEncoded)

        val (customer, shouldSendWelcomeEmail) = dbSession.connected(TransactionSerializable) {
          loginViaFacebook(registrationName.toString, registrationEmail.toString, user_id.toString)
        }
        if (shouldSendWelcomeEmail) {
          dbSession.connected(TransactionSerializable) {
            customer.sendNewCustomerEmail()
          }
        }
        session.put(WebsiteControllers.customerIdKey, customer.id.toString)
        new Redirect(Utils.lookupUrl("WebsiteControllers.getRootEndpoint").url)
      }
      case _ => error("smthg wrong with facebook redirect")
    }
  }

  /**
   * Gets Account-Customer pair, creating them as necessary.
   * Persists fbUserId on the Account returned, and registrationName on the related Customer.
   */
  private def loginViaFacebook(registrationName: String, registrationEmail: String, user_id: String): (Customer, Boolean) = {
    val accountOption = accountStore.findByEmail(registrationEmail)
    val account = accountOption match {
      case Some(a) => a.copy(fbUserId = Some(user_id)).save()
      case None => Account(email = registrationEmail, fbUserId = Some(user_id)).save()
    }

    val (customer, shouldSendWelcomeEmail) = account.customerId match {
      case Some(custId) => (customerStore.findById(custId).get, false)
      case None => (Customer(), true)
    }
    val savedCustomer = customer.copy(name = registrationName).save()
    if (account.customerId == None) account.copy(customerId = Some(savedCustomer.id)).save()
    (savedCustomer, shouldSendWelcomeEmail)
  }

  /**
   * Validates that data actually came from Facebook and not a malicious party.
   */
  private def validateFacebookCallback(algorithm: String, dataEncoded: String, signatureEncoded: String) {
    // check algorithm
    if (algorithm.toUpperCase != "HMAC-SHA256") {
      throw new RuntimeException("Facebook authentication algorithm was not HMAC-SHA256")
    }
    // check signature
    val expected_sig = getHash(dataEncoded)
    if (expected_sig != replaceUrlChars(signatureEncoded)) {
      throw new RuntimeException("Facebook authentication signature was incorrect")
    }
  }

  private def getHash(baseString: String) = {
    val secretStr = Play.configuration.getProperty(fbAppSecretKey)
    val keyBytes = secretStr.getBytes("UTF-8")
    val mac = Mac.getInstance("HMACSHA256")
    val secretKey = new SecretKeySpec(keyBytes, mac.getAlgorithm)
    mac.init(secretKey)
    val text = baseString.getBytes("UTF-8")
    val encodedText = mac.doFinal(text)
    new String(Codec.encodeBASE64(encodedText)).replace("=", "")
  }

  private def replaceUrlChars(str: String): String = {
    str.replace('-', '+').replace('_', '/')
  }
}