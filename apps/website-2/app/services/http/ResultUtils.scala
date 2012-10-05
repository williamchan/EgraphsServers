package services.http

import play.api.http.HeaderNames.SET_COOKIE
import play.api.mvc.Cookies
import play.api.mvc.PlainResult
import play.api.mvc.Result
import play.api.mvc.Session

object ResultUtils {

  implicit def resultToRichResult(result: Result) = RichResult(result)

  case class RichResult(result: Result) {
    val session: Session = {
      val maybeSession = result match {
        case result: PlainResult => {
          sessionFromPlainResult(result)
        }
        case other => None
      }

      maybeSession.getOrElse(Session.emptyCookie)
    }

    private def sessionFromPlainResult(plainResult: PlainResult): Option[Session] = {
      val maybeCookieString: Option[String] = plainResult.header.headers.get(SET_COOKIE)
      maybeCookieString.map(cookies => Session.decodeFromCookie(Cookies.decode(cookies).find(_.name == Session.COOKIE_NAME)))
    }
  }
}