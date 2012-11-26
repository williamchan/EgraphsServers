package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form

/** Defines some mostly-minimal requirements for ToyBox */
trait ToyBoxBase {
  // Routes for log-in end points
  def getLoginRoute:  Call
  def postLoginRoute: Call

  // Configuration helpers
  def authUsername: String
  def authPassword: String
  def isPrivate: Boolean
  
  // Cookie configuration helpers
  def initialRequestCookieName: String
  def authCookieName: String
  def authTimeout: Int
  def authPath: String
  def authDomain: Option[String]

  val loginForm: Form[(String, String)]
}

trait ToyBoxGlobal {
  def onRouteRequest(request: RequestHeader): Option[Handler]
}

/** Defines the endpoints for ToyBox login page */
trait ToyBoxController extends Controller {
  def getLogin:  Action[AnyContent]
  def postLogin: Action[AnyContent]
}



trait ToyBoxAuthenticator {
  def publicPaths: Seq[String]

  def isAuthorized(request: RequestHeader): Boolean
  def isPublicResourceRequest(request: RequestHeader): Boolean
  def isValidLogin(usrPwdPair: (String, String)): Boolean

  protected def authenticate(action: Action[AnyContent]): Action[AnyContent]
  protected def makeAuthCookie(request: RequestHeader): Cookie
}

/** Store of keys for querying configuration */
object ToyBoxConfigKeys {
  /** Key for username (String) of ToyBox login */
  val usrKey         = "username"

  /** Key for password of (String) ToyBox login */
  val pwdKey         = "password"

  /** Key for flag (Boolean) to turn ToyBox privacy on or off */
  val privateKey     = "is-private"

  /** Key for name of cookie (String) in which the initial requests 
   *  method and path will be stored */
  val initRequestKey = "initial-request-cookie"

  /** Key for name of authentication cookie (String) */
  val authCookieKey  = "auth-cookie"

  /** Key for setting authentication timeout (Integer) in seconds */
  val authTimeoutKey = "auth-timeout"

  /** Key for setting authentication cookie path (String) */
  val authPathKey    = "auth-path"

  /** Key for setting authentication cookie domain (String) */
  val authDomainKey  = "auth-domain"
}