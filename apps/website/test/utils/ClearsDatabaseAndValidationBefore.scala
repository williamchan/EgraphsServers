package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import play.data.validation.Validation
import services.blobs.Blobs
import services.AppConfig
import services.db.{TransactionSerializable, DBSession, Schema}
import services.cache.Cache

/**
 * Mix this trait in to your Suite class to make sure that Play Validation
 * and all DB data are scrubbed in between test runs.
 */
trait ClearsDatabaseAndValidationBefore extends BeforeAndAfterEach { this: Suite =>
  override def beforeEach() {
    super.beforeEach()
    Validation.clear()
    AppConfig.instance[Blobs].scrub()
//    AppConfig.instance[Cache].clear() // todo(wchan): Why does this stop all unit tests from running?
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      AppConfig.instance[Schema].scrub()
    }
  }
}
