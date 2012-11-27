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
import akka.agent.Agent
import play.api.libs.concurrent.Akka

@RunWith(classOf[JUnitRunner])
class CatalogStarsQueryingTests extends EgraphsUnitTest {
  "CatalogStarsQuerying" should "grab from the source actor and do nothing else if the actor found results" in new EgraphsTestApplication {
    println("AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHh")
    // Set up so that the source actor has the mock stars information
    val mockStars = IndexedSeq(CatalogStar(1,
      "name",
      None,
      "imageUrl",
      "marketplaceImageUrl",
      "storefrontUrl",
      false,
      false,
      10,
      100))

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars(_) =>
          throw new RuntimeException("I should never have been called.")
      }
    }

    // Run the test and check expectations
    withActorUnderTest(updateActorInstance) { updateActor =>
      withAgent(fakeCatalogStarAgent(mockStars)) { catalogStarAgent =>
        println("AHHHHHHHHHHHHHHHhh  5 " + catalogStarAgent())
        val catstars = queryWithUpdateActor(catalogStarAgent, updateActor).apply() //TODO REMOVE
        println("AHHHHHHHHHHHHHHHhh  7 " + catstars)
        queryWithUpdateActor(catalogStarAgent, updateActor).apply() should be (mockStars)
      }
    }
  }

  it should "tell the source actor to update if it didn't already find anything" in new EgraphsTestApplication {
    // Set up so that the source actor has no star information but the update actor does.
    val mockStars = IndexedSeq(CatalogStar(1,
      "name",
      None,
      "imageUrl",
      "marketplaceImageUrl",
      "storefrontUrl",
      false,
      false,
      10,
      100))

    lazy val updateActorInstance = new Actor {
      protected def receive = {
        case UpdateCatalogStars(catalogStarsAgent) =>
          catalogStarsAgent send mockStars
          sender ! "Done!"
      }
    }

    // The query should produce the value from the update actor.
    withActorUnderTest(updateActorInstance) { updateActor =>
      withAgent(fakeCatalogStarAgent(IndexedSeq.empty[CatalogStar])) { catalogStarAgent =>
        queryWithUpdateActor(catalogStarAgent, updateActor).apply() should be (mockStars)
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

  private def queryWithUpdateActor(catalogStarAgent: Agent[IndexedSeq[CatalogStar]], updateActor: ActorRef) = {
    println("AHHHHHHHHHHHHHHHhh  6 " + catalogStarAgent())
    new CatalogStarsQuerying {
      override def catalogStarAgent = catalogStarAgent
      override def catalogStarUpdateActor: ActorRef = updateActor
    }
  }
}
