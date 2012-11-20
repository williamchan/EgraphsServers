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

/** Store of keys for querying configuration */
object ToyBoxConfigKeys {
  val usrKey         = "username"
  val pwdKey         = "password"
  val privateKey     = "is-private"
  val initRequestKey = "initial-request-cookie"
  val authCookieKey  = "auth-cookie"
  val authTimeoutKey = "auth-timeout"
  val authPathKey    = "auth-path"
  val authDomainKey  = "auth-domain"
}