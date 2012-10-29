package services.mvc.celebrity

import models.Celebrity

/**
 * Trait that describes the behavior of converting a Celebrity to a richer class that contains
 * functionality for transforming the domain object into its various faces on the consumer website.
 * (e.g. [[models.frontend.landing.CatalogStar]]).
 *
 * The canonical implementation of this trait is CelebrityViewConversions'
 *
 */
private[mvc] trait CelebrityViewConverting {
  implicit def celebrityAsCelebrityViewConversions(celebrity: Celebrity): CelebrityViewConversions
}
