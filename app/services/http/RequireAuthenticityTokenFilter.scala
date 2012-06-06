package services.http

import play.mvc.Scope.Session
import play.mvc.Http.Request
import play.Play
import play.mvc.results.{Forbidden}
import com.google.inject.{Provider, Inject}
import java.util.Properties

/**
 * Only executes its `continue` block if the request contains a valid authenticity token, as implemented
 * by Play. Helps protect against CSRF.
 *
 * Easiest to use from a Controller since it already has implicit [[play.mvc.Scope.Session]] and
 * [[play.mvc.Http.Request]] types
 *
 * Usage:
 *
 * {{{
 *   val requireAuthenticityToken = services.AppConfig.instance[RequireAuthenticityTokenFilter]
 *   val forbiddenOrResult = requireAuthenticityToken {
 *     println("I can safely service my request here")
 *   }
 * }}}
 *
 */
trait RequireAuthenticityTokenFilter {
  def apply[A](continue: => A)(implicit session: Session, request: Request): Either[Forbidden, A]
}


class RequireAuthenticityTokenFilterProvider @Inject()(@PlayId playId: String)
  extends Provider[RequireAuthenticityTokenFilter]
{
  //
  // Public members
  //
  def apply(doCheck: Boolean = true): RequireAuthenticityTokenFilter = {
    // Only ever check if both (1) we're not in test mode and (2) doCheck is true
    (playId, doCheck) match {
      case ("test", _) | (_, false) => new DontRequireAuthenticityToken
      case (_, true) => new DoRequireAuthenticityToken
    }
  }


  //
  // Provider[RequireAuthenticityTokenFilter] members
  //
  override def get = {
    this.apply(doCheck=true)
  }

}


/** Implementation that actually checks the response */
private[http] class DoRequireAuthenticityToken @Inject() extends RequireAuthenticityTokenFilter {
  override def apply[A](continue: => A)(implicit session: Session, request: Request) = {
    if (request.params.get("authenticityToken") == session.getAuthenticityToken) {
      Right(continue)
    }
    else {
      Left(new Forbidden("Bad authenticity token"))
    }
  }
}


/** Implementation that just processes the request */
private[http] class DontRequireAuthenticityToken @Inject() extends RequireAuthenticityTokenFilter {
  override def apply[A](continue: => A)(implicit session: Session, request: Request) = {
    Right(continue)
  }
}