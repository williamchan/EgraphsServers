package controllers

import play.mvc.{Controller, Before}
import models.Celebrity

trait RequiresCelebrityName { this: Controller =>
  //
  // Protected methods
  //
  protected def celebrity: Celebrity = {
    _celebrity.get
  }

  //
  // Private implementation
  //
  private val _celebrity = new ThreadLocal[Celebrity]

  @Before(priority=20)
  protected def ensureRequestHasCelebrity = {
    Option(params.get("celebrityName")) match {
      case None =>
        throw new IllegalStateException(
          """
          Investigate this. This should never have happened since our routes are supposed
          to ensure that celebrityName is present.
          """
        )

      case Some(celebrityName) =>
        Celebrity.findByName(celebrityName) match {
          case None =>
            NotFound("No celebrity named \"" + celebrityName + "\"")

          case Some(celebrity) =>
            _celebrity.set(celebrity)
            Continue
        }
    }
  }
}