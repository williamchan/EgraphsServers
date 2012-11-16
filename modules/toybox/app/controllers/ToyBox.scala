package controllers

import play.api.GlobalSettings
import play.api.Configuration
import play.api.Plugin
import play.api.Play
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.data.format.Formats._
import play.api.mvc._
import play.api.mvc.Results._


/**
 * ToyBox. This is the only type needed to add privacy to your app. To use it:
 *   1. Mix it into an object in the application
 *   2. Route the ToyBox controllers as desired in routes file
 *   3. Implement abstract methods (optionally, may override components)
 *   4. Enable the plugin located at [your ToyBox object].plugin
 */

// TODO: is there any reason not to just extend them all instead of self typing?
trait ToyBox extends TBPlugin with TBController with TBConfig with TBRoutes {
}

trait TBRoutes {
  /**
   * Route to the getLogin method of the ToyBoxController
   * ex: controllers.routes.[YourToyBoxObject].Controllers.getLogin
   */
  def getLoginRoute: Call

  /**
   * Route to the postLogin method of the ToyBoxController
   * ex: controllers.routes.[YourToyBoxObject].Controllers.postLogin
   */
  def postLoginRoute: Call
}

trait TBConfig {
  /**
   * Name of authentication cookie
   */
  def authenticatedCookie: String = 
    Play.current.configuration.getString("toybox-authenticatedCookie").getOrElse(
      "toybox-authenticated")

  def initialRequestCookie: String = 
    Play.current.configuration.getString("toybox-initialRequestCookie").getOrElse(
      "toybox-initialRequest")
  def isPrivate: Boolean = 
    Play.current.configuration.getBoolean("toybox-isPrivate").getOrElse(true)
  def authenticationTimeout: Int = // 20 minute default
    Play.current.configuration.getInt("toybox-authenticationTimeout").getOrElse(20*60) 
  def actualUsername: String
  def actualPassword: String
}

/**
 * This trait handles requests to the application. To extend its functionality,
 * override tbPlugin value with an implementation of ToyBoxPlugin.
 */
trait TBPlugin { this: TBRoutes with TBConfig =>
  val tbPlugin: ToyBoxPlugin = DefaultTBPlugin
  val verbose = false

  abstract class ToyBoxPlugin extends GlobalSettings {
    val loginAssetsPath = "/assets/public/toybox-assets"

    override def onRouteRequest(req: RequestHeader): Option[Handler] = { 
      if (verbose) {
        println("""request received: """ + req)
        println("""method:           """ + req.method)
        println("""uri:              """ + req.uri)
        println("""remote address:   """ + req.remoteAddress)
      }

      req.cookies.get(authenticatedCookie) match {
        // request is from authenticated user
        case Some(Cookie(name, value, _, path, domain, secure, httpOnly)) => {
          
          // Helper for renewing authenticated cookie on PlainResults
          def tryRenewingAuthentication(result: Result): Result = result match {
            case plainResult: PlainResult => 
              plainResult.withCookies( 
                new Cookie(name, value, authenticationTimeout,  path, domain, secure, httpOnly)
              )
            case _ => result
          }
          
          // compose default handler to renew authentication if possible
          super.onRouteRequest(req) match {
            // Actions and Cached handlers may be used to renew authentication
            case Some(action: Action[AnyContent]) => Some( Action { implicit request =>
               tryRenewingAuthentication( action(request) )
            })

            // Other handlers currently cannot set cookies, to return as is
            case defaultMaybeHandler => defaultMaybeHandler
          }
        }

        // No authentication cookie present in request, so redirect to login as needed
        case None => {
          val needsAuthentication = isPrivate &&      // app is in private mode
            req.path != getLoginRoute.url &&          // request is not for login page
            !req.path.startsWith(loginAssetsPath)     // request is not for login asset
            
          if (needsAuthentication) redirectToLogin(req)
          else                     super.onRouteRequest(req)          }
      }
    }

    def redirectToLogin(req: RequestHeader): Option[Handler]
    def isAuthenticated(req: RequestHeader): Boolean
  }

  object DefaultTBPlugin extends ToyBoxPlugin {
    def redirectToLogin(req: RequestHeader) = {
      Some(Action {
        Redirect(getLoginRoute).withCookies(makeInitialRequestCookie(req))
      }) // login
    }

    def isAuthenticated(req: RequestHeader): Boolean = {
      // make sure req is not null and it has the cookie
      req != null && req.cookies != null && 
        req.cookies.get(authenticatedCookie) != None   
    }

    /**
     *
     */
    private def makeInitialRequestCookie(req: RequestHeader): Cookie = {
      // TODO: should path be something betterer?
      new Cookie(initialRequestCookie, req.method+req.path, -1, "/", None,  false, false)
    }
  }
}

// IN PROGRESS
trait TBController { this: TBRoutes with TBConfig =>
  val tbController: ToyBoxController = DefaultTBController

  abstract class ToyBoxController extends Controller {
    def getLogin: Action[AnyContent]
    def postLogin: Action[AnyContent]
  }

  object DefaultTBController extends ToyBoxController {
    val loginForm = Form(
      tuple( 
        "username" -> Forms.of[String],             // not necessarily required
        "password" -> nonEmptyText                  // must be non-empty
      )
    )

    def getLogin = Action {
      // This is the default login page, takes the route to POST form submission to
      // 
      Ok(views.html.login(postLoginRoute, loginForm))
    }

    def postLogin = Action { implicit request =>
      loginForm.bindFromRequest.fold(
        // login failed due to form submission errors

        // BadRequest(route.to.login(postLoginRoute, formErrors))
        formErrors => BadRequest(views.html.login(postLoginRoute, formErrors)),

        // return the desired page or login page with failure message
        loginAttempt => {
          if (checkCredentials(loginAttempt)){
              // success
              // TODO: pull destination from a cookie
              Redirect(parseInitialRequestCookie(request)).withCookies(
                new Cookie(authenticatedCookie, "what should i be?", authenticationTimeout,
                  "/", None, false, false)
              )
          } else {
              // failure
              // TODO: add errors; should look like
              // val loginErrors: Form[(String, String)] = ...
              // BadRequest(route.to.login(postLoginRoute, loginErrors))
              BadRequest(views.html.login(postLoginRoute, loginForm))
          }
        }
      )
    }

    private def checkCredentials(usrPwdPair: (String, String)) = {
      usrPwdPair._1 == actualUsername && usrPwdPair._2 == actualPassword
    }

    private def parseInitialRequestCookie(req: Request[_]): Call = 
      req.cookies.get(initialRequestCookie) match {
      case Some(Cookie(_, value, _, _, _, _, _)) =>
          val method = value.takeWhile(_ != '/')
          val dest = value.stripPrefix(method)
          new Call(method, dest)
      case _ => new Call("GET", "/")

    }
  }
}