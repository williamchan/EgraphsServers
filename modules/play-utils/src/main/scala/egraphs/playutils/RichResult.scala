package egraphs.playutils

import play.api.mvc.{Session, Result, PlainResult, Cookies}
import play.api.http.HeaderNames.SET_COOKIE

/**
 * "Pimp-my-library" of Play's PlainResult type. Gives direct access to
 * the Result's session. Helpful when trying to affect the outgoing result in action
 * composition.
 *
 * Usage: 
 * {{{
 *   def insertSomethingIntoTheSessionOnTheWayOut[A](action: Action[A]): Action[A] {
 *     import egraphs.playutils.RichResult._
 *     Action { request =>
 *       val result = action(request)
 *       result.withSession(result.session ++ ("newSessionKey" -> "newSessionValue"))
 *     }
 *   }
 * }}}
 */
class RichResult(result: Result) {
  
  /**
   * Returns the [[play.api.mvc.Result]]'s session, if it had one. Otherwise
   * returns an empty session.
   */
  val session: Session = {
    val maybeSession = result match {
      case result: PlainResult => {
        sessionFromPlainResult(result)
      }
      case other => None
    }

    maybeSession.getOrElse(Session.emptyCookie)
  }

  /**
   * If the result was a plain ol' HTTP result then it sets a new session cookie
   * into it. Otherwise just returns the result without anything altered (because
   * a session wouldn't make sense in that context)
   */
  def withSession(session: Session): Result = {
    result match {
      case plainResult: PlainResult => plainResult.withSession(session)
      case other => other
    }
  }

  //
  // Private members
  //
  private def sessionFromPlainResult(plainResult: PlainResult): Option[Session] = {
    val maybeCookieString: Option[String] = plainResult.header.headers.get(SET_COOKIE)
    maybeCookieString.map(cookies => Session.decodeFromCookie(Cookies.decode(cookies).find(_.name == Session.COOKIE_NAME)))
  }
}

object RichResult {
  implicit def resultToRichResult(result: Result) = new RichResult(result)
}