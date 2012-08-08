package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import services.db.{Schema, DBSession}
import services.AppConfig
import junitx.util.PrivateAccessor
import collection.mutable.ArrayBuffer
import org.squeryl.Table

/**
 * Mix in to a test suite class to ensure that a Squeryl database transaction
 * is available before each test case runs.
 */
trait DBTransactionPerTest extends BeforeAndAfterEach { this: Suite =>
  override def beforeEach() {
    super.beforeEach()
    DBSession.init()
  }

  override def afterEach() {
    DBSession.rollback()
    super.afterEach()
  }

}
