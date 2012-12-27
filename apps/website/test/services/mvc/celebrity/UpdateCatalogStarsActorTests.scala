package services.mvc.celebrity

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.specs2.mock.Mockito
import com.google.inject.Inject
import akka.actor.ActorRef
import akka.agent.Agent
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import models.frontend.landing.CatalogStar
import models.CelebrityStore
import services.cache.CacheFactory
import services.cache.NamespacedCache
import services.db.DBSession
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars
import services.AppConfig
import utils.TestHelpers.withActorUnderTest
import utils.ClearsCacheBefore
import utils.EgraphsUnitTest
import utils.TestHelpers
import play.api.libs.concurrent.Akka

@RunWith(classOf[JUnitRunner])
class UpdateCatalogStarsActorTests extends EgraphsUnitTest {
  implicit val timeout: Timeout = 5 seconds

  import UpdateCatalogStarsActorTests.Dependencies

  "UpdateCatalogStarsActor" should "transmit what it finds in the cache to the parameterized actor" in new EgraphsTestApplication {
    // Set up so that the cache produces a sequence with one mock CatalogStar
    val deps = newDeps.withSpiedCache
    val mockStars = IndexedSeq(mock[CatalogStar])
    deps.cache.get(anyString)(any[Manifest[IndexedSeq[CatalogStar]]]) returns Some(mockStars)

    // Run test and check expectations
    updateResultsForActorWithDepsShouldBe(mockStars, deps)
  }

  it should "grab from the database if it finds nothing in the cache" in new EgraphsTestApplication {
    // Set up so that cache produces no CatalogStars and the database produces a mock celebrity
    // that converts into our mock CatalogStar.
    val deps = newDeps.withSpiedCache.withMockCelebrityStore.copy(viewConverting = mock[CelebrityViewConverting])

    val mockCatalogStars = IndexedSeq(mock[CatalogStar])

    deps.cache.get(anyString)(any[Manifest[IndexedSeq[CatalogStar]]]) returns None
    deps.celebrityStore.getCatalogStars returns mockCatalogStars

    // Perform the test and check expectations
    updateResultsForActorWithDepsShouldBe(mockCatalogStars, deps)
    there was one(deps.cache).set(
      UpdateCatalogStarsActor.resultsCacheKey,
      mockCatalogStars,
      UpdateCatalogStarsActor.updatePeriod.toSeconds.toInt
    )
  }

  //
  // Private members
  //
  private def updateResultsForActorWithDepsShouldBe
    (stars: IndexedSeq[CatalogStar], deps: Dependencies)
    (implicit app: play.api.Application)
  {
    withUpdateCatalogStarsActorAndRecipient(deps) {
      (actor) =>
        withAgent(Agent(IndexedSeq.empty[CatalogStar])(Akka.system)) { catalogStarsAgent =>
          Await.result(actor ask UpdateCatalogStars(catalogStarsAgent), 5 seconds)

          catalogStarsAgent.get should be(stars)
        }
    }
  }

  private def newDeps = {
    AppConfig.instance[Dependencies]
  }

  private def withUpdateCatalogStarsActorAndRecipient[ResultT]
  (deps: Dependencies = newDeps)
  (operation: (ActorRef) => ResultT)
  (implicit app: play.api.Application)
  : ResultT = {
    lazy val actorInstance = new UpdateCatalogStarsActor(
      deps.db, deps.cacheFactory, deps.celebrityStore, deps.viewConverting
    )

    withActorUnderTest(actorInstance) { actor =>
      operation(actor)
    }
  }
}

object UpdateCatalogStarsActorTests extends Mockito {

  private[UpdateCatalogStarsActorTests] case class Dependencies @Inject()(
    db: DBSession,
    cacheFactory: CacheFactory,
    celebrityStore: CelebrityStore,
    viewConverting: CelebrityViewConverting
  ) {
    def withSpiedCache: Dependencies = {
      val mockCache = spy(cacheFactory.applicationCache)
      val mockCacheFactory = mock[CacheFactory]

      mockCacheFactory.applicationCache returns mockCache
      this.copy(cacheFactory = mockCacheFactory)
    }

    def withMockCelebrityStore: Dependencies = {
      this.copy(celebrityStore = mock[CelebrityStore])
    }

    def cache: NamespacedCache = {
      cacheFactory.applicationCache
    }
  }
}
