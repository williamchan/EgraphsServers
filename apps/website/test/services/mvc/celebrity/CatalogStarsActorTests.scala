package services.mvc.celebrity

import utils.EgraphsUnitTest
import models.frontend.landing.CatalogStar
import akka.actor.ActorRef
import utils.TestHelpers.withActorUnderTest

class CatalogStarsActorTests extends EgraphsUnitTest {
  "CatalogStarsActor.GetCatalogStars" should "serve no celebs if it has not yet been provided any" in {
    withCatalogStarsActor {
      actor =>
        (actor !! CatalogStarsActor.GetCatalogStars) should be(Some(None))
    }
  }

  "CatalogStarsActor.GetCatalogStars" should "serve celebs if they have been previously provided" in {
    withCatalogStarsActor {
      actor =>
      // Set up
        val mockCeleb = mock[CatalogStar]
        val mockNewCelebs = IndexedSeq(mockCeleb)

        actor ! CatalogStarsActor.SetCatalogStars(mockNewCelebs)

        // Run test
        val maybeCatalogStars = actor !! CatalogStarsActor.GetCatalogStars

        // Check expectations
        maybeCatalogStars should be(Some(Some(mockNewCelebs)))
    }
  }

  "CatalogStarsActor" should "update with new celebs properly" in {
    withCatalogStarsActor {
      actor =>
      // Set up
        val mockFirstCeleb = mock[CatalogStar]
        val mockUpdatedCeleb = mock[CatalogStar]

        actor ! CatalogStarsActor.SetCatalogStars(IndexedSeq(mockFirstCeleb))
        actor ! CatalogStarsActor.SetCatalogStars(IndexedSeq(mockUpdatedCeleb))

        // Run test
        val maybeCatalogStars = actor !! CatalogStarsActor.GetCatalogStars

        // Check expectations
        maybeCatalogStars should be(Some(Some(IndexedSeq(mockUpdatedCeleb))))
    }
  }

  //
  // Private members
  //
  private def withCatalogStarsActor[ResultT](operation: ActorRef => ResultT): ResultT = {
    withActorUnderTest[CatalogStarsActor, ResultT](operation)
  }
}
