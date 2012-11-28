package services.mvc.celebrity

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import akka.agent.Agent
import akka.util.Timeout.durationToTimeout
import akka.util.duration.intToDurationInt
import models.frontend.landing.CatalogStar
import play.api.libs.concurrent.Akka
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars
import utils.TestHelpers.withActorUnderTest
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class CatalogStarsQueryingTests extends EgraphsUnitTest {
  "CatalogStarsQuerying" should "grab from the source actor and do nothing else if the actor found results" in new EgraphsTestApplication {
    // Set up so that the source actor has the mock stars information
    val mockStars = IndexedSeq(mock[CatalogStar])

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars(_) =>
          throw new RuntimeException("I should never have been called.")
      }
    }

    // Run the test and check expectations
    withActorUnderTest(updateActorInstance) { updateActor =>
      withAgent(fakeCatalogStarAgent(mockStars)) { catalogStarAgent =>
        val catstars = queryWithUpdateActor(catalogStarAgent, updateActor).apply() //TODO REMOVE
        queryWithUpdateActor(catalogStarAgent, updateActor).apply() should be(mockStars)
      }
    }
  }

  it should "tell the source actor to update if it didn't already find anything" in new EgraphsTestApplication {
    // Set up so that the source actor has no star information but the update actor does.
    val mockStars = IndexedSeq(mock[CatalogStar])

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars(catalogStarsAgent) =>
          catalogStarsAgent send mockStars
          catalogStarsAgent.await(10 seconds)
          sender ! "Done!"
      }
    }

    // The query should produce the value from the update actor.
    withActorUnderTest(updateActorInstance) { updateActor =>
      withAgent(fakeCatalogStarAgent(IndexedSeq.empty[CatalogStar])) { catalogStarAgent =>
        queryWithUpdateActor(catalogStarAgent, updateActor).apply() should be(mockStars)
      }
    }
  }

  //
  // Private members
  //
  private def fakeCatalogStarAgent(catalogStars: IndexedSeq[CatalogStar]) = {
    import play.api.Play.current
    Agent(catalogStars)(Akka.system)
  }

  private def queryWithUpdateActor(starAgent: Agent[IndexedSeq[CatalogStar]], updateActor: ActorRef) = {
    new CatalogStarsQuerying {
      override def catalogStarAgent = starAgent
      override def catalogStarUpdateActor: ActorRef = updateActor
    }
  }
}
