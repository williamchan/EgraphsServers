package egraphs.playutils

import play.api.mvc.{Session, Result, PlainResult, Cookies}
import play.api.http.HeaderNames.SET_COOKIE
import play.api.mvc.AsyncResult
import play.api.mvc.RequestHeader
import play.api.mvc.CookieBaker
import play.api.mvc.Flash

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
  lazy val session: Option[Session] = {
    bakeCookieFromPlainResult(this.plainResult, baker=Session)
  }

  /**
   * If the result was a plain ol' HTTP result then it sets a new session cookie
   * into it. Otherwise throws an exception.
   */
  def withSession(session: Session): Result = {
    this.plainResult.withSession(session)
  }
  
  /**
   * Returns the [[play.api.mvc.Result]]'s flash as represented in its
   * SET_COOKIE header, if it had one. Otherwise returns None.
   * 
   * Throws an exception if this was not a PlainResult.
   */
  lazy val flash: Option[Flash] = {
    bakeCookieFromPlainResult(this.plainResult, baker=Flash)
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
  private def bakeCookieFromPlainResult[T <: AnyRef](result: PlainResult, baker: CookieBaker[T]): Option[T] = {
    for (
      setCookieString <- result.header.headers.get(SET_COOKIE);
      cookie <- Cookies.decode(setCookieString).find(_.name == baker.COOKIE_NAME)
    ) yield {
      baker.decodeFromCookie(Some(cookie))
    }
  }
  
  private lazy val plainResult:PlainResult = {
    result match {
      case plainResult: PlainResult => plainResult
      case other => throw new RuntimeException("Can not access flash of a non-PlainResult.")
    }
  } 
}

object RichResult {
  implicit def resultToRichResult(result: Result) = new RichResult(result)
}