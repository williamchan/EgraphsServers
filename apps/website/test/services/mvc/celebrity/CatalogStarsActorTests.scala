package services.mvc.celebrity

import utils.EgraphsUnitTest
import models.frontend.landing.CatalogStar
import akka.actor.ActorRef
import utils.TestHelpers.withActorUnderTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.dispatch.Await
import akka.util.duration._
import akka.pattern.ask
import akka.util.Timeout
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class CatalogStarsActorTests extends EgraphsUnitTest {
  implicit val timeout:Timeout = 5 seconds
  
  "CatalogStarsActor.GetCatalogStars" should "serve no celebs if it has not yet been provided any" in new EgraphsTestApplication {
    withCatalogStarsActor {
      actor =>
        Await.result(actor ask CatalogStarsActor.GetCatalogStars, 5 seconds) should be (None)
    }
  }

  "CatalogStarsActor.GetCatalogStars" should "serve celebs if they have been previously provided" in new EgraphsTestApplication {
    withCatalogStarsActor {
      actor =>
      // Set up
        val mockCeleb = mock[CatalogStar]
        val mockNewCelebs = IndexedSeq(mockCeleb)

        actor ! CatalogStarsActor.SetCatalogStars(mockNewCelebs)

        // Run test
        val maybeCatalogStars = Await.result(actor ask CatalogStarsActor.GetCatalogStars, 5 seconds)

        // Check expectations
        maybeCatalogStars should be (Some(mockNewCelebs))
    }
  }

  "CatalogStarsActor" should "update with new celebs properly" in new EgraphsTestApplication {
    withCatalogStarsActor {
      actor =>
      // Set up
        val mockFirstCeleb = mock[CatalogStar]
        val mockUpdatedCeleb = mock[CatalogStar]

        actor ! CatalogStarsActor.SetCatalogStars(IndexedSeq(mockFirstCeleb))
        actor ! CatalogStarsActor.SetCatalogStars(IndexedSeq(mockUpdatedCeleb))

        // Run test
        val maybeCatalogStars = Await.result(actor ask CatalogStarsActor.GetCatalogStars, 5 seconds)

        // Check expectations
        maybeCatalogStars should be (Some(IndexedSeq(mockUpdatedCeleb)))
    }
  }

  //
  // Private members
  //
  private def withCatalogStarsActor[ResultT]
    (operation: ActorRef => ResultT)
    (implicit app: play.api.Application)
  : ResultT = {
    withActorUnderTest(AppConfig.instance[CatalogStarsActor])(operation)
  }
}
