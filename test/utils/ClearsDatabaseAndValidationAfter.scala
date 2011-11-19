package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import play.data.validation.Validation
import libs.Blobs

/**
 * Mix this trait in to your Suite class to make sure that Play Validation
 * and all DB data are scrubbed in between test runs.
 */
trait ClearsDatabaseAndValidationAfter extends BeforeAndAfterEach { this: Suite =>
  override def afterEach {
    Validation.clear()
    db.Schema.scrub()
    Blobs.scrub()

    super.afterEach()
  }
}
