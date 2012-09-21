package services.http

import play.api.mvc.Request
import play.api.mvc.Results.Forbidden
import com.google.inject.{Provider, Inject}
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._


/**
 * Only executes its `action` block if the request contains a valid authenticity token, as implemented
 * by Play. Helps protect against CSRF.
 *
 * Easiest to use from a Controller since it already has implicit [[play.mvc.Scope.Session]] and
 * [[play.api.mvc.Request]] values.
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
   * @param action the code block to execute if the request is verified as authentic
   * @param session the current request's session
   * @param request the current request
   * @tparam A return type of `action`
   * @return either the return value of the action on the right, or a Forbidden on the left
   *     if no valid authenticity token was provided.
   */
  def apply[A](action: Action[A]): Action[A]
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
  override def apply[A](action: Action[A]): Action[A] = {
    Action(action.parser) { implicit request =>
      // TODO: PLAY20 migration. Fix authenticity token implementation. Right now it doesn't
      //   even test against the session. We may have to manually provide an authenticity token
      //   for each session.
      Form(single("authenticityToken" -> text)).bindFromRequest.fold(
        errors => Forbidden("Bad authenticity token"),
        token => action(request)
      )
    }
  }
}


/** Implementation that just processes the `action` */
private[http] class DontRequireAuthenticityToken @Inject() extends RequireAuthenticityTokenFilter {
  override def apply[A](action: Action[A]): Action[A] = {
    action
  }
}
