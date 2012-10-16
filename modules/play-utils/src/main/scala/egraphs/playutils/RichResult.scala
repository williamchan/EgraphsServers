package egraphs.playutils

import play.api.mvc.{Session, Result, PlainResult, Cookies}
import play.api.http.HeaderNames.SET_COOKIE
import play.api.mvc.AsyncResult
import play.api.mvc.RequestHeader

/**
 * "Pimp-my-library" of Play's PlainResult type. Gives direct access to
 * the Result's SET_COOKIE cookie as a [[play.api.mvc.Session]] object. Helpful when trying 
 * to affect the outgoing result in action composition.
 *
 * Usage: 
 * {{{
 *   def insertSomethingIntoTheSessionOnTheWayOut[A](action: Action[A]): Action[A] {
 *     import egraphs.playutils.RichResult._
 *     Action { implicit request =>
 *       val result = action(request)
 *       result.addingToSession("newSessionKey" -> "newSessionValue")
 *     }
 *   }
 * }}}
 */
class RichResult(result: Result) {
  
  /**
   * Returns the [[play.api.mvc.Result]]'s session as represented in its
   * SET_COOKIE header, if it had one. Otherwise returns None.
   * 
   * Throws an exception if this was not a PlainResult.
   */
  val session: Option[Session] = {    
    result match {
      case result: PlainResult => sessionFromPlainResult(result)
      case other => throw new RuntimeException("Can not access session of a non-PlainResult.")
    }
  }

  /**
   * If the result was a plain ol' HTTP result then it sets a new session cookie
   * into it. Otherwise throws an exception.
   */
  def withSession(session: Session): Result = {
    result match {
      case plainResult: PlainResult => plainResult.withSession(session)
      case other => throw new RuntimeException("Can not write session of a non-PlainResult.")
    }
  }
  
  /**
   * Adds additional data to the session. Appends to the SET_COOKIE header if one already
   * existed in the result for the session cookie. Otherwise, grabs the existing session
   * from an implicit [[play.api.mvc.RequestHeader]] and appends to that.
   */
  def addingToSession
    (newSessionTuples: (String, String)*)
    (implicit requestHeader: RequestHeader)
    : Result = 
  {
    val originalSessionMap = this.session.getOrElse(requestHeader.session).data 
    this.withSession(Session(originalSessionMap ++ newSessionTuples.toMap))
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