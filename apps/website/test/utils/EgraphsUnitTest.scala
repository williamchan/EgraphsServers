package utils

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.specs2.mock.Mockito
import services.AppConfig
import services.db.DBSession

/**
 * Convenience method provides most generally used traits for a scalatest
 */
trait EgraphsUnitTest extends UnitFlatSpec with ShouldMatchers with Mockito {
  protected val db = AppConfig.instance[DBSession]
}
