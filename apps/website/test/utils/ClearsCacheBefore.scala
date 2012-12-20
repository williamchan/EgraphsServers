package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import services.blobs.Blobs
import services.AppConfig
import services.cache.CacheFactory

/**
 * Mix this trait in to your Suite class to make sure that cache data 
 * is scrubbed in between test runs.
 */
trait ClearsCacheBefore extends BeforeAndAfterEach { this: Suite with EgraphsUnitTest =>
  override def beforeEach() {
    super.beforeEach()
    new EgraphsTestApplication {
      AppConfig.instance[CacheFactory].applicationCache.clear()
    }
  }
}
