package egraphs.authtoken

import egraphs.playutils.ResultUtils.RichResult
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.{Forbidden}
import play.api.mvc.RequestHeader
import play.api.mvc.Action
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.mvc.Request

/**
 * A pair of action compositions that protect your POST methods against CSRF submissions
 * and provide authenticity tokens to your templates.
 */
private [authtoken] trait AuthenticityTokenActionComposition {
  import AuthenticityToken.authTokenKey

  /**
   * Protect any POST controller by composing this action. It will not respect any request
   * that failed to provide the correct authenticity token via safeForm.
   * 
   * Usage:
   * {{{
   *   def myControllerThatChargesBankAccounts = AuthenticityToken.requireInSubmission {
   *     Action {
   *       // Do whatever you want here. It will only happen if the user provided an
   *       // authenticity token.
   *     }
   *   }
   * }}}
   */
  def requireInSubmission[A](action: Action[A]): Action[A] = {
    Action(action.parser) { implicit request =>
      // Read the auth token from both the session and the request and make sure they match.
      val maybeSafeResult = for {
        sessionToken <- request.session.get(authTokenKey)
        formToken <- authTokenFromRequest(request)  
        if (sessionToken == formToken)
      } yield {
        action(request)
      }

      // Reset the auth token if any part of our checks failed.
      maybeSafeResult.getOrElse {
        logInvalidAttempt(request)
        Forbidden("Invalid authenticity token; someone may be trying to hack your connection to egraphs.com")
          .withSession(request.session + (authTokenKey -> newAuthToken.value))
      }
    }
  }

  /**
   * Provides an authenticity token to controllers that render templates with token-protected
   * forms.
   *
   * Usage:
   * {{{
   *   def myControllerThatGeneratesSafeForms = AuthenticityToken.makeAvailable() { implicit token =>
   *     Action {
   *       // Render any forms that require implicit AuthenticityTokens here. This will be any form
   *       // that contains a call to AuthenticityToken.safeForm or AuthenticityToken.hiddenInput
   *     }
   *   }
   * }}}
   */
  def makeAvailable[A]
    (parser: BodyParser[A] = parse.anyContent)
    (actionFactory: AuthenticityToken => Action[A]): Action[A] = 
  {
    Action(parser) { implicit request =>
      val maybeToken = for (sessionToken <- request.session.get(authTokenKey)) yield {
        new AuthenticityToken(sessionToken)
      }
      val token = maybeToken.getOrElse(newAuthToken)
      val result = actionFactory(token).apply(request)
      // Put the token we just generated in the session if there wasn't already
      // a token there.
      if (maybeToken.isDefined) {
        result
      } else {
        result.addingToSession(authTokenKey -> token.value)
      }
    }
  }

  //
  // Abstract members
  //
  protected def newAuthTokenString: String

  //
  // Private members
  //
  private def newAuthToken: AuthenticityToken = {
    new AuthenticityToken(newAuthTokenString)
  }
  
  private def logInvalidAttempt(request: Request[_]) {
    play.api.Logger.info(
      "Invalid authenticity token \"" + authTokenFromRequest(request).getOrElse("") + "\" during request to " + request.uri +
      " from client " + request.remoteAddress + ". Expected \"" + request.session.get(authTokenKey).getOrElse("") + "\"."
    )    
  }
  
  private def authTokenFromRequest(request: Request[_]): Option[String] = {
    Form(single(authTokenKey -> text)).bindFromRequest()(request).apply(authTokenKey).value
  }
}
