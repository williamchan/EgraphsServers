package controllers.website

import controllers.WebsiteControllers
import java.util.Properties
import models.{Account, Customer, CustomerStore, AccountStore}
import play.mvc.Controller
import play.mvc.results.Redirect
import services.db.{TransactionSerializable, DBSession}
import services.http.ControllerMethod
import services.Utils
import services.social.Facebook
import services.logging.Logging

private[controllers] trait GetFacebookLoginCallbackEndpoint extends Logging { this: Controller =>

  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def playConfig: Properties
  protected def facebookAppId: String

  private val fbAppSecretKey = "fb.appsecret"

  /**
   * This is a middle step in the Facebook Oauth flow. For information on what each parameter is, see Facebook Oauth
   * docs at https://developers.facebook.com/docs/authentication/server-side/
   * and at https://developers.facebook.com/docs/reference/dialogs/oauth/
   */
  def getFacebookLoginCallback(state: String,
                               code: Option[String] = None,
                               error: Option[String] = None,
                               error_reason: Option[String] = None,
                               error_description: Option[String] = None) = controllerMethod() {

    validateFacebookCallbackState(state)

    code match {
      case Some(fbCode) => {
        val accessToken = Facebook.getFbAccessToken(code = fbCode, facebookAppId = facebookAppId, fbAppSecret = playConfig.getProperty(fbAppSecretKey))
        val fbUserInfo = Facebook.getFbUserInfo(accessToken = accessToken)
        val (customer, shouldSendWelcomeEmail) = dbSession.connected(TransactionSerializable) {
          loginViaFacebook(registrationName = fbUserInfo(Facebook._name).toString, registrationEmail = fbUserInfo(Facebook._email).toString, user_id = fbUserInfo(Facebook._id).toString)
        }
        if (shouldSendWelcomeEmail) {
          dbSession.connected(TransactionSerializable) {
            customer.account.withResetPasswordKey.save()
            customer.sendNewCustomerEmail()
          }
        }
        new Redirect(reverse(WebsiteControllers.getAccountSettings).url)

      }
      case _ => {
        log("Facebook Oauth flow halted. error =  " + error.getOrElse("") +
          ", error_reason = " + error_reason.getOrElse("") +
          ", error_description = " + error_description.getOrElse(""))
        new Redirect(reverse(WebsiteControllers.getLogin).url)
      }
    }
  }

  /**
   * Gets Account-Customer pair, creating them as necessary, and logs the customer into the session.
   *
   * @param registrationName name of Facebook user. Persisted to the Customer if the Customer is new.
   * @param registrationEmail email of Facebook user. This is used to find or create the Account.
   * @param user_id id of Facebook user. This is persisted to the Account.
   * @return Account and Customer as a tuple
   */
  private def loginViaFacebook(registrationName: String, registrationEmail: String, user_id: String): (Customer, Boolean) = {
    val accountOption = accountStore.findByEmail(registrationEmail)
    val account = accountOption match {
      case Some(a) => a.copy(fbUserId = Some(user_id)).save()
      case None => Account(email = registrationEmail, fbUserId = Some(user_id)).save()
    }

    val (customer, shouldSendWelcomeEmail) = account.customerId match {
      case Some(custId) => {
        (customerStore.get(custId), false)
      }
      case None => {
        val customer = account.createCustomer(name = registrationName).save()
        account.copy(customerId = Some(customer.id)).save()
        (customer, true)
      }
    }

    session.put(WebsiteControllers.customerIdKey, customer.id.toString)
    (customer, shouldSendWelcomeEmail)
  }

  /**
   * @param state checked to be the same state parameter as in Facebook.getFbOauthUrl to guard against CSRF attacks.
   */
  private def validateFacebookCallbackState(state: String) {
    if (state != session.get(Facebook._fbState)) {
      throw new RuntimeException("Facebook authentication failed to verify 'state' parameter")
    }
  }
}

object GetFacebookLoginCallbackEndpoint {
  def getCallbackUrl = Utils.lookupAbsoluteUrl("WebsiteControllers.getFacebookLoginCallback").url
}
