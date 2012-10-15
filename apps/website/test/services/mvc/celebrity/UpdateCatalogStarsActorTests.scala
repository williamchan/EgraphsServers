package services.mvc.celebrity

import utils.{ClearsCacheAndBlobsAndValidationBefore, TestHelpers, EgraphsUnitTest}
import services.db.DBSession
import models.{Celebrity, CelebrityStore}
import services.cache.{NamespacedCache, CacheFactory}
import TestHelpers.withActorUnderTest
import com.google.inject.Inject
import services.AppConfig
import akka.actor.ActorRef
import models.frontend.landing.CatalogStar
import org.specs2.mock.Mockito
import services.mvc.celebrity.UpdateCatalogStarsActor.UpdateCatalogStars
import services.mvc.celebrity.CatalogStarsActor.GetCatalogStars

class UpdateCatalogStarsActorTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {

  import UpdateCatalogStarsActorTests.Dependencies

  "UpdateCatalogStarsActor" should "transmit what it finds in the cache to the parameterized actor" in {
    // Set up so that the cache produces a sequence with one mock CatalogStar
    val deps = newDeps.withSpiedCache
    val mockStars = IndexedSeq(mock[CatalogStar])
    deps.cache.get(anyString)(any[Manifest[IndexedSeq[CatalogStar]]]) returns Some(mockStars)

    // Run test and check expectations
    updateResultsForActorWithDepsShouldBe(Some(mockStars), deps)
  }

  "UpdateCatalogStarsActor" should "grab from the database if it finds nothing in the cache" in {
    // Set up so that cache produces no CatalogStars and the database produces a mock celebrity
    // that converts into our mock CatalogStar.
    val deps = newDeps.withSpiedCache.withMockCelebrityStore.copy(viewConverting = mock[CelebrityViewConverting])

    val mockCelebs = List(mock[Celebrity])
    val mockViewConverter = mock[CelebrityViewConversions]
    val mockCatalogStars = IndexedSeq(mock[CatalogStar])

    deps.cache.get(anyString)(any[Manifest[IndexedSeq[CatalogStar]]]) returns None
    deps.celebrityStore.getPublishedCelebrities returns mockCelebs
    deps.viewConverting.celebrityAsCelebrityViewConversions(mockCelebs(0)) returns mockViewConverter

    mockViewConverter.asCatalogStar returns mockCatalogStars(0)

    // Perform the test and check expectations
    updateResultsForActorWithDepsShouldBe(Some(mockCatalogStars), deps)
    there was one(deps.cache).set(
      UpdateCatalogStarsActor.resultsCacheKey,
      mockCatalogStars,
      UpdateCatalogStarsActor.updatePeriodSeconds
    )
  }


  //
  // Private members
  //
  private def updateResultsForActorWithDepsShouldBe
  (stars: Option[IndexedSeq[CatalogStar]], deps: Dependencies) {
    withUpdateCatalogStarsActorAndRecipient(deps) {
      (actor, recipient) =>
        actor !! UpdateCatalogStars(recipient)

        recipient !! GetCatalogStars should be(Some(stars))
    }
  }

  private def newDeps = {
    AppConfig.instance[Dependencies]
  }

  private def withUpdateCatalogStarsActorAndRecipient[ResultT]
  (deps: Dependencies = newDeps)
  (operation: (ActorRef, ActorRef) => ResultT)
  : ResultT = {
    lazy val actorInstance = new UpdateCatalogStarsActor(
      deps.db, deps.cacheFactory, deps.celebrityStore, deps.viewConverting
    )

    withActorUnderTest(actorInstance) {
      actor =>
        withActorUnderTest[CatalogStarsActor, ResultT] {
          recipient: ActorRef =>
            operation(actor, recipient)
        }
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
