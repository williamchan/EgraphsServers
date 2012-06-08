package controllers.website

import models.Celebrity
import utils.EgraphsUnitTest

class GetRootEndpointTests extends EgraphsUnitTest {
  "GetRootEndpoint.ModelViewConversions" should "correctly convert a Celebrity to a FeaturedStar" in {
    import GetRootEndpoint.ModelViewConversions._

    Celebrity(publicName=Some("Wizzle Chan")).asFeaturedStar match {
      case Some(featuredStar) =>
        featuredStar.name should be ("Wizzle Chan")
        featuredStar.storefrontUrl should be ("/Wizzle-Chan")

      case somethingElse =>
        fail("Expected a correctly configured FeaturedStar. Instead got: " + somethingElse)
    }

    // A celebrity without a publicName can't be a featured star. You're in the big leagues now.
    Celebrity(publicName=None).asFeaturedStar should be (None)
  }
}
