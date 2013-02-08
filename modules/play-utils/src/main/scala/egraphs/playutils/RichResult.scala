package egraphs.playutils

import scala.language.postfixOps
import scala.language.implicitConversions
import org.joda.time.DateTimeConstants
import play.api.mvc._
import play.api.http.HeaderNames.SET_COOKIE
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

object ResultUtils {
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
  implicit class RichResult(result: Result) {

    /**
     * Extracts the Status code of this Result value. Copied from Play20's
     * play.api.test.Helpers.status(of: Result)
     */
    def status: Option[Int] = result match {
      case PlainResult(status, _) => Some(status)
      case AsyncResult(futureResult) => (new RichResult(Await.result(futureResult, 1 minute))).status
    }

    /**
     * Returns the [[play.api.mvc.Result]]'s session as represented in its
     * SET_COOKIE header, if it had one. Otherwise returns None.
     *
     * Throws an exception if this was not a PlainResult.
     */
    lazy val session: Option[Session] = {
      bakeCookieFromResult(this.result, baker = Session)
    }

    /**
     * Returns the [[play.api.mvc.Result]]'s flash as represented in its
     * SET_COOKIE header, if it had one. Otherwise returns None.
     *
     * Throws an exception if this was not a PlainResult.
     */
    lazy val flash: Option[Flash] = {
      bakeCookieFromResult(this.result, baker = Flash)
    }

    /**
     * Adds additional data to the session. Appends to the SET_COOKIE header if one already
     * existed in the result for the session cookie. Otherwise, grabs the existing session
     * from an implicit [[play.api.mvc.RequestHeader]] and appends to that.
     */
    def addingToSession(newSessionTuples: (String, String)*)(implicit requestHeader: RequestHeader): Result =
      {
        val originalSessionMap = this.session.getOrElse(requestHeader.session).data
        result.withSession(Session(originalSessionMap ++ newSessionTuples.toMap))
      }

    def removeFromSession(removedKeys: String*)(implicit requestHeader: RequestHeader): Result =
      {
        val originalSessionMap = this.session.getOrElse(requestHeader.session).data
        result.withSession(Session(originalSessionMap -- removedKeys))
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

    //TODO: Find out if this is even still necessary in Play 2.1
    private def bakeCookieFromResult[T <: AnyRef](result: Result, baker: CookieBaker[T]): Option[T] = {
      result match {
        case plainResult: PlainResult => bakeCookieFromPlainResult(plainResult, baker)
        //FIXME: Play 2.1 Use something that doesn't have to await if possible
        case asyncResult: AsyncResult => Await.result(asyncResult.result.map { promisedResult =>
          bakeCookieFromResult(promisedResult, baker)
        }, 30 seconds)
      }
    }
  }
}