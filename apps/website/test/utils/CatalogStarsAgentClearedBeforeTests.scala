package utils

import org.scalatest.{ Suite, BeforeAndAfterEach }
import services.db.DBSession
import akka.util.duration._
import services.mvc.celebrity.CatalogStarsAgent

/**
 * Mix in to a test suite class to ensure that a CatalogStarsAgent
 * is cleared before each test.
 */
trait CatalogStarsAgentClearedBeforeTests extends BeforeAndAfterEach { this: Suite with EgraphsUnitTest =>
  override def beforeEach() {
    super.beforeEach()
    new EgraphsTestApplication {
      clearCatalogStarsAgent()
    }
  }

  def clearCatalogStarsAgent() = {
    CatalogStarsAgent.singleton send IndexedSeq()
    CatalogStarsAgent.singleton.await(10 seconds)
  }
}
