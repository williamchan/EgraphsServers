package controllers

import play.api._
import play.api.mvc._

/** Defines some mostly-minimal requirements for ToyBox */
trait ToyBoxBase { this: ToyBoxController with GlobalSettings =>
  // Routes for log-in end points
  def maybeGetLoginRoute:  Option[Call]
  def maybePostLoginRoute: Option[Call]

  // Configuration helpers
  def authUsername: String
  def authPassword: String
  def isPrivate: Boolean
  def publicPaths: Seq[String]
  
  // Cookie configuration helpers
  def initialRequestCookieName: String
  def authCookieName: String
  def authTimeout: Int
  def authPath: String
  def authDomain: Option[String]
}


/** Defines the endpoints for ToyBox login page */
trait ToyBoxController extends Controller { this: ToyBox =>
  def getLogin:  Action[AnyContent]
  def postLogin: Action[AnyContent]
}

/** Store of keys for querying configuration 
 */
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