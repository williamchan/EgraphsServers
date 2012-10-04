package controllers.website

import controllers.WebsiteControllers
import java.util.Properties
import models.{Account, Customer, CustomerStore, AccountStore}
import play.api._
import play.api.mvc._
import play.api.mvc.Results.Redirect
import services.db.{TransactionSerializable, DBSession}
import services.http.ControllerMethod
import services.Utils
import services.social.Facebook
import services.logging.Logging
import services.http.EgraphsSession.Conversions._
import play.api.data._
import play.api.data.Forms._

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
  def getFacebookLoginCallback = controllerMethod() {  
    Action { implicit request =>
      implicit val session = request.session
      
      val fbForm = Form(
        tuple(
          "state" -> text,
          "code" -> optional(text),
          "error" -> optional(text),
          "error_reason" -> optional(text),
          "error_description" -> optional(text)
        )
      )

      val (state, code, error, error_reason, error_description) = fbForm.bindFromRequest.get

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
              val account = customer.account.withResetPasswordKey.save()
              Customer.sendNewCustomerEmail(account = account, verificationNeeded = false, mail = customer.services.mail)
            }
          }

          Redirect(controllers.routes.WebsiteControllers.getAccountSettings).withSession(
            session.withCustomerId(customer.id)
          )
        }
        case _ => {
          log("Facebook Oauth flow halted. error =  " + error.getOrElse("") +
            ", error_reason = " + error_reason.getOrElse("") +
            ", error_description = " + error_description.getOrElse(""))
          Redirect(controllers.routes.WebsiteControllers.getLogin)
        }
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
  private def loginViaFacebook(registrationName: String, registrationEmail: String, user_id: String)(implicit session: Session): (Customer, Boolean) = {
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

    (customer, shouldSendWelcomeEmail)
  }

  /**
   * @param state checked to be the same state parameter as in Facebook.getFbOauthUrl to guard against CSRF attacks.
   */
  private def validateFacebookCallbackState(state: String)(implicit session: Session) {
    session.get(Facebook._fbState) match {
      case None => throw new RuntimeException("There is no Facebook authentication state to verify against 'state' parameter")
      case Some(fbState) =>
        if (state != session.get(Facebook._fbState)) {
          throw new RuntimeException("Facebook authentication failed to verify 'state' parameter")
        }
    }
  }
}

object GetFacebookLoginCallbackEndpoint {
  def getCallbackUrl(implicit request: RequestHeader): String = {
    controllers.routes.WebsiteControllers.getFacebookLoginCallback().absoluteURL(secure=true)
  }
}
