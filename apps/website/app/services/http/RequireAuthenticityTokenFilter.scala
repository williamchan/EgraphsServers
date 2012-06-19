package services.http

import play.mvc.Scope.Session
import play.mvc.Http.Request
import play.mvc.results.{Forbidden}
import com.google.inject.{Provider, Inject}

/**
 * Only executes its `operation` block if the request contains a valid authenticity token, as implemented
 * by Play. Helps protect against CSRF.
 *
 * Easiest to use from a Controller since it already has implicit [[play.mvc.Scope.Session]] and
 * [[play.mvc.Http.Request]] values.
 *
 * Usage:
 * {{{
 *   val requireAuthenticityToken = services.AppConfig.instance[RequireAuthenticityTokenFilter]
 *   val forbiddenOrResult = requireAuthenticityToken {
 *     println("The code in here is safe from CSRF")
 *   }
 * }}}
 *
 */
trait RequireAuthenticityTokenFilter {

  /**
   *
   * @param operation the code block to execute if the request is verified as authentic
   * @param session the current request's session
   * @param request the current request
   * @tparam A return type of `operation`
   * @return either the return value of the operation on the right, or a Forbidden on the left
   *     if no valid authenticity token was provided.
   */
  def apply[A](operation: => A)(implicit session: Session, request: Request): Either[Forbidden, A]
}


/**
 * Factory for RequireAuthenticityTokenFilters
 *
 * @param playId the current play ID (e.g. test, staging, live, demo)
 */
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
  override def apply[A](operation: => A)(implicit session: Session, request: Request) = {
    if (request.params.get("authenticityToken") == session.getAuthenticityToken) {
      Right(operation)
    }
    else {
      Left(new Forbidden("Bad authenticity token"))
    }
  }
}


/** Implementation that just processes the `operation` */
private[http] class DontRequireAuthenticityToken @Inject() extends RequireAuthenticityTokenFilter {
  override def apply[A](operation: => A)(implicit session: Session, request: Request) = {
    Right(operation)
  }
}