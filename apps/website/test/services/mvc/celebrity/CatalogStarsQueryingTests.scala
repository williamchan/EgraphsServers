package services.mvc.celebrity

import utils.EgraphsUnitTest
import models.frontend.landing.CatalogStar
import services.mvc.celebrity.CatalogStarsActor.{SetCatalogStars, GetCatalogStars}
import akka.actor.{ActorRef, Actor}
import utils.TestHelpers.withActorUnderTest
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars

class CatalogStarsQueryingTests extends EgraphsUnitTest {

  "CatalogStarsQuerying" should "grab from the source actor and do nothing else if the actor found results" in {
    // Set up so that the source actor has the mock stars information
    val mockStars = IndexedSeq(mock[CatalogStar])

    lazy val sourceActorInstance = new Actor {
      protected def receive = {
        case GetCatalogStars =>
          self.channel ! Some(mockStars)
      }
    }

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        throw new RuntimeException("I should never have been called.")
      }
    }

    // Run the test and check expectations
    withActorUnderTest(sourceActorInstance) { sourceActor =>
      withActorUnderTest(updateActorInstance) { updateActor =>
        queryWithGetAndUpdateActors(sourceActor, updateActor).apply() should be (mockStars)
      }
    }
  }

  "CatalogStarsQuerying" should "tell the source actor to update if it didn't already find anything" in {
    // Set up so that the source actor has no star information but the update actor does.
    val mockStars = IndexedSeq(mock[CatalogStar])

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars(recipientActor) =>
          recipientActor ! SetCatalogStars(mockStars)
          self.channel ! "Done!"
      }
    }

    // The query should produce the value from the update actor.
    withActorUnderTest(new CatalogStarsActor) { sourceActor =>
      withActorUnderTest(updateActorInstance) { updateActor =>
        queryWithGetAndUpdateActors(sourceActor, updateActor).apply() should be (mockStars)
      }
    }
  }

  //
  // Private members
  //
  private def queryWithGetAndUpdateActors(sourceActor: ActorRef, updateActor: ActorRef) = {
    new CatalogStarsQuerying {
      def catalogStarActor: ActorRef = sourceActor

      def catalogStarUpdateActor: ActorRef = updateActor
    }
  }
}
