package controllers.website

import controllers.WebsiteControllers
import models.{Account, Customer, CustomerStore, AccountStore}
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.mvc.Results.Redirect
import services.db.{TransactionSerializable, DBSession}
import services.http.ControllerMethod
import services.ConsumerApplication
import services.social.Facebook
import services.logging.Logging
import services.config.ConfigFileProxy
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._
import services.http.EgraphsSession.Key._
import play.api.data._
import play.api.data.Forms._
import services.email.AccountCreationEmail

private[controllers] trait GetFacebookLoginCallbackEndpoint extends Logging { this: Controller =>

  protected def dbSession: DBSession
  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore
  protected def customerStore: CustomerStore
  protected def config: ConfigFileProxy
  protected def facebookAppId: String
  protected def consumerApp: ConsumerApplication

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
          val fbCallbackUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getFacebookLoginCallback().url)
          val accessToken = Facebook.getFbAccessToken(code = fbCode, facebookAppId = facebookAppId, 
              fbAppSecret = config.fbAppsecret, fbCallbackUrl = fbCallbackUrl)
          val fbUserInfo = Facebook.getFbUserInfo(accessToken = accessToken)

          val maybeRedirectSuccess = for {
            registrationName <- (fbUserInfo \ Facebook._name).asOpt[String]
            registrationEmail <- (fbUserInfo \ Facebook._email).asOpt[String]
            userId <- (fbUserInfo \ Facebook._id).asOpt[String]
          } yield {
            val (customer, shouldSendWelcomeEmail) = dbSession.connected(TransactionSerializable) {
              loginViaFacebook(
                registrationName = registrationName,
                registrationEmail = registrationEmail,
                user_id = userId)
            }
            if (shouldSendWelcomeEmail) {
              dbSession.connected(TransactionSerializable) {
                val account = customer.account.withResetPasswordKey.save()
                AccountCreationEmail(account = account, verificationNeeded = false).send()
              }
            }

            // Find out whether the user is logging in via Facebook to complete their celebrity request
            val redirectCall: Call = dbSession.connected(TransactionSerializable) {
              request.session.requestedStarRedirectOrCall(
                customer.id,
                controllers.routes.WebsiteControllers.getAccountSettings)
            }

            Redirect(redirectCall).withSession(
              session
                .withCustomerId(customer.id)
                .removeRequestedStar
                .removeRequestStarTargetUrl
            ).withCookies(Cookie(HasSignedUp.name, true.toString, maxAge = Some(EgraphsSession.COOKIE_MAX_AGE)))
          }

          maybeRedirectSuccess.getOrElse(redirectAndLogError(fbUserInfo))
        }
        case _ => {
          log("Facebook Oauth flow halted. error =  " + error.getOrElse("") +
            ", error_reason = " + error_reason.getOrElse("") +
            ", error_description = " + error_description.getOrElse(""))
          Redirect(controllers.routes.WebsiteControllers.getLogin())
        }
      }
    }
  }

  private def redirectAndLogError(fbUserInfo: JsValue): Result = {
    error("Facebook did not respond with expected user info format: " + Json.stringify(fbUserInfo))
    Redirect(controllers.routes.WebsiteControllers.getLogin())
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
        if (state != fbState) throw new RuntimeException("Facebook authentication failed to verify 'state' parameter")
    }
  }
}
