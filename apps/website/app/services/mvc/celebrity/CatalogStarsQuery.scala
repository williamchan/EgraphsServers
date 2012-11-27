package services.mvc.celebrity

/**
 * Public class for quickly retrieving the current CatalogStars. See the controller that
 * serves up CatalogStars for example usage. Currently that is GetRootConsumerEndpoint.scala.
 */
class CatalogStarsQuery extends CatalogStarsQuerying {
  //
  // CatalogStarsQuerying members
  //
  override protected def catalogStarAgent = {
    CatalogStarsAgent.singleton
  }

  override protected def catalogStarUpdateActor = {
    UpdateCatalogStarsActor.singleton
  }
}
