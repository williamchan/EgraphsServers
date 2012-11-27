package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form

/** Defines configuration options for ToyBox */
trait ToyBoxBase { this: GlobalSettings =>
  // Routes for log-in end points
  def getLoginRoute:  Call
  def postLoginRoute: Call
  def assetsRoute: String => Call

  // Configuration helpers
  def authUsername: String
  def authPassword: String
  def isPrivate: Boolean
  def publicAccessConditions: Seq[RequestHeader => Boolean]
  
  // Cookie configuration helpers
  def initialRequestCookieName: String
  def authCookieName: String
  def authTimeoutInSeconds: Int
  def authPath: String
  def authDomain: Option[String]

  val loginForm: Form[(String, String)]
}


trait ToyBoxController extends Controller {
  def getLogin:  Action[AnyContent]
  def postLogin: Action[AnyContent]
}


trait ToyBoxAuthenticator {
  def isAuthorized(request: RequestHeader): Boolean
  def isPublicResourceRequest(request: RequestHeader): Boolean

  protected def authenticate(action: Action[AnyContent]): Action[AnyContent]
  protected def makeAuthCookie(request: RequestHeader): Cookie
}