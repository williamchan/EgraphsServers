package services.mvc.celebrity

import utils.EgraphsUnitTest
import models.frontend.landing.CatalogStar
import akka.actor.{ActorRef, Actor}
import utils.TestHelpers.withActorUnderTest
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.util.duration._
import org.scalatest.BeforeAndAfterEach
import utils.CatalogStarsAgentClearedBeforeTests

@RunWith(classOf[JUnitRunner])
class CatalogStarsQueryingTests extends EgraphsUnitTest with CatalogStarsAgentClearedBeforeTests {
  "CatalogStarsQuerying" should "grab from the source actor and do nothing else if the actor found results" in new EgraphsTestApplication {
    // Set up so that the source actor has the mock stars information
    val mockStars = IndexedSeq(mock[CatalogStar])
    
    CatalogStarsAgent.singleton send mockStars
    CatalogStarsAgent.singleton.await(10 seconds)

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars =>
          throw new RuntimeException("I should never have been called.")
      }
    }

    // Run the test and check expectations
    withActorUnderTest(updateActorInstance) { updateActor =>
      queryWithUpdateActor(updateActor).apply() should be (mockStars)
    }
  }

  it should "tell the source actor to update if it didn't already find anything" in new EgraphsTestApplication {
    // Set up so that the source actor has no star information but the update actor does.
    val mockStars = IndexedSeq(mock[CatalogStar])

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars =>
          CatalogStarsAgent.singleton send mockStars
          sender ! "Done!"
      }
    }

    // The query should produce the value from the update actor.
    withActorUnderTest(updateActorInstance) { updateActor =>
      queryWithUpdateActor(updateActor).apply() should be (mockStars)
    }
  }

  //
  // Private members
  //
  private def queryWithUpdateActor(updateActor: ActorRef) = {
    new CatalogStarsQuerying {
      def catalogStarUpdateActor: ActorRef = updateActor
    }
  }
}
