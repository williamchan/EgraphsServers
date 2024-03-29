package services.http.filters

import play.api.mvc.Request
import play.api.mvc.Results.Forbidden
import com.google.inject.{Provider, Inject}
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import services.inject.InjectionProvider
import egraphs.authtoken.AuthenticityToken

/**
 * Only executes its `action` block if the request contains a valid authenticity token, as implemented
 * by Play. Helps protect against CSRF.
 *
 * Usage:
 * {{{
 *   val requireAuthenticityToken = services.AppConfig.instance[RequireAuthenticityTokenFilter]
 *   
 *   def someControllerMethod = requireAuthenticityToken {
 *     Action {
 *       println("The code in here is safe from CSRF")
 *       Ok
 *     }
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
class RequireAuthenticityTokenFilterProvider @Inject()()
  extends InjectionProvider[RequireAuthenticityTokenFilter]
{
  //
  // Public members
  //
  def apply(doCheck: Boolean = true): RequireAuthenticityTokenFilter = {
    doCheck match {
      case false => new DontRequireAuthenticityToken
      case true => new DoRequireAuthenticityToken
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
    AuthenticityToken.requireInSubmission(action)
  }
}


/** Implementation that just processes the `action` */
private[http] class DontRequireAuthenticityToken @Inject() extends RequireAuthenticityTokenFilter {
  override def apply[A](action: Action[A]): Action[A] = {
    action
  }
}
