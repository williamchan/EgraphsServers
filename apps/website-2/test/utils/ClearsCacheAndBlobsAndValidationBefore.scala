package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import play.data.validation.Validation
import services.blobs.Blobs
import services.AppConfig
import services.cache.CacheFactory

/**
 * Mix this trait in to your Suite class to make sure that Play Validation
 * and all blob and cache data are scrubbed in between test runs.
 */
trait ClearsCacheAndBlobsAndValidationBefore extends BeforeAndAfterEach { this: Suite with EgraphsUnitTest =>
  override def beforeEach() {
    super.beforeEach()
    new EgraphsTestApplication {
      AppConfig.instance[Blobs].scrub()
      AppConfig.instance[CacheFactory].applicationCache.clear()
    }
  }
}
