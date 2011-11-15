package controllers

import models.Celebrity
import play.mvc.{Before, Controller}

/**
 * Provides a high-priority @Before interception that requires the request
 * to have a celebrityId field that is valid and authorized. Also provides a celebrity
 * field to access the Celebrity object associated with that id.
 *
 * Mix in to any Controller that already RequiresAuthenticatedAccount
 */
trait RequiresCelebrity { this: Controller with RequiresAuthenticatedAccount =>
  //
  // Public methods
  //
  protected def celebrity: Celebrity = {
    _celebrity.get
  }

  //
  // Private implementation
  //
  private val _celebrity = new ThreadLocal[Celebrity]

  @Before(priority=20)
  protected def ensureRequestIsCelebrity = {
    Option(params.get("celebrityId")) match {
      case None =>
        Error("Celebrity ID was required but not provided")

      case Some(celebrityId) if celebrityId == "me" =>
        account.celebrityId match {
          case None =>
            Error("This request requires a celebrity account.")

          case Some(accountCelebrityId) =>
            _celebrity.set(Celebrity.findById(accountCelebrityId).get)
            Continue
        }

      case Some(celebrityId) =>
        Error(
          "Unexpected request for celebrityId \""+celebrityId+"\". Only \"me\" is currently supported."
        )
    }
  }
}