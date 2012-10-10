package egraphs.authtoken

import egraphs.playutils.RichResult._

import play.api.mvc.Action
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.libs.Crypto
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.{Forbidden}
import play.api.mvc.RequestHeader
import play.api.templates.{Html, HtmlFormat}

class AuthenticityToken(private[authtoken] val value: String)

object AuthenticityToken 
  extends AuthenticityTokenActionComposition 
  with AuthenticityTokenFormHelpers 
{
  //
  // AuthenticityTokenActionComposition members
  //
  override protected def newAuthTokenString = {
    Crypto.sign(java.util.UUID.randomUUID.toString)
  }

  //
  // Private members
  //
  private[authtoken] val authTokenKey = "authenticityToken"  
}

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
      val maybeResult = for {
        sessionToken <- request.session.get(authTokenKey)
        formToken <- Form(single(authTokenKey -> text)).bindFromRequest.apply(authTokenKey).value
        if (sessionToken == formToken)
      } yield {
        action(request)
      }

      // Reset the auth token if any part of our checks failed.
      maybeResult.getOrElse {
        Forbidden.withSession(request.session + (authTokenKey -> newAuthToken.value))
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
    Action(parser) { request =>
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
        result.withSession(result.session + (authTokenKey -> token.value))
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
}

private[authtoken] trait AuthenticityTokenFormHelpers {
  import AuthenticityToken.authTokenKey

  def safeForm(attributes: (String, String)*)(contents: => Html)(implicit token: AuthenticityToken): Html = {
    val attributeStrings = for (nameValue <- attributes) yield {
      nameValue._1 + "=\"" + nameValue._2+ "\""
    }

    val attributeHtml = HtmlFormat.escape(attributeStrings.mkString(" "))    

    Html("<form ") + attributeHtml + Html(">") + hiddenInput + contents + Html("</form>")
  }

  def hiddenInput(implicit token: AuthenticityToken): Html = {
    Html("<input type='hidden' name='" + authTokenKey + "'>" + token.value + "</input>")
  }
}
