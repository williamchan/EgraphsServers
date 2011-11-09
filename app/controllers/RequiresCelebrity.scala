package controllers

import models.Celebrity
import play.mvc.{Before, Controller}

/**
 * Provides a high-priority @Before interception that requires the request
 * to have a celebrityId field that is valid and authorized.
 *
 * Mix in to any Controller that already RequiresAuthenticatedAccount
 */
trait RequiresCelebrity { this: Controller with RequiresAuthenticatedAccount =>
  //
  // Public methods
  //
  def celebrity = {
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
        for (celebrityId <- account.celebrityId;
             celebrity <- Celebrity.findById(celebrityId)) _celebrity.set(celebrity)
        Continue

      case celebrityId =>
        new IllegalArgumentException(
          "Unexpected request for celebrityId \""+celebrityId+"\". Only \"me\" is currently supported."
        )
    }
    Continue
  }
}