package controllers

import models.Celebrity
import play.mvc.{Before, Controller}

trait RequiresCelebrity { this: Controller with RequiresAuthenticatedAccount =>

  private val _celebrity = new ThreadLocal[Celebrity]

  def celebrity = {
    _celebrity.get
  }

  @Before(priority=20)
  def ensureRequestIsCelebrity = {
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