package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import play.data.validation.Validation
import services.blobs.Blobs
import services.AppConfig
import services.db.{TransactionSerializable, DBSession, Schema}

/**
 * Mix this trait in to your Suite class to make sure that Play Validation
 * and all DB data are scrubbed in between test runs.
 */
trait ClearsDatabaseAndValidationAfter extends BeforeAndAfterEach { this: Suite =>
  override def afterEach {
    Validation.clear()
    AppConfig.instance[Blobs].scrub()
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      AppConfig.instance[Schema].scrub()
    }

    super.afterEach()
  }
}
