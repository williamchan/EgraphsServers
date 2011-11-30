package controllers

import play.mvc.{Controller, Before}
import models.Celebrity

/**
 * Ensures that any controllers that mix in this trait will only be processed if the requested
 * URL has a valid celebrity url slug.
 */
trait RequiresCelebrityName { this: Controller =>
  //
  // Protected methods
  //
  /** Access the located product (for use in controller methods only) */
  protected def celebrity: Celebrity = {
    _celebrity.get
  }

  //
  // Private implementation
  //
  private val _celebrity = new ThreadLocal[Celebrity]

  @Before(priority=RequiresCelebrityName.interceptPriority)
  protected def ensureRequestHasCelebrity = {
    Option(params.get("celebrityUrlSlug")) match {
      case None =>
        throw new IllegalStateException(
          """
          celebrityUrlSlug parameter was not provided. This should never have happened since our routes are supposed
          to ensure that it is present.
          """
        )

      case Some(celebrityUrlSlug) =>
        Celebrity.findByUrlSlug(celebrityUrlSlug) match {
          case None =>
            NotFound("No celebrity with url \"" + celebrityUrlSlug + "\"")

          case Some(celebrity) =>
            _celebrity.set(celebrity)
            Continue
        }
    }
  }
}

object RequiresCelebrityName {
  final val interceptPriority = 20
}