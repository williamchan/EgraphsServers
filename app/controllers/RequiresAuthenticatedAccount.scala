package controllers

import play.mvc.{Before, Controller}
import models._

/**
 * Provides a high-priority @Before interception that
 * requires the request be linked to an authenticated account.
 *
 * Currently authenticates via basic auth.
 *
 */
trait RequiresAuthenticatedAccount { this: Controller =>
  //
  // Public methods
  //
  /** Retrieve the authenticated Account */
  protected def account:Account = {
    _account.get
  }

  //
  // Private implementation
  //
  private val _account = new ThreadLocal[Account]

  @Before(priority=10)
  def ensureRequestAuthenticated = {
    import models.ApiRequest.Conversions._

    request.authenticatedAccount match {
      case Right(theAccount) =>
        _account.set(theAccount)
        Continue
        
      case Left(_: AccountAuthenticationError) =>
        Forbidden("Email/password information was incorrect.")
    }
  }
}
