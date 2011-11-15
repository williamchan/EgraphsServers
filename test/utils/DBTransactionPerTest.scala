package utils

import org.scalatest.{Suite, BeforeAndAfterEach}
import org.squeryl.{Session, SessionFactory}

/**
 * Mix in to a test suite class to ensure that a Squeryl database transaction
 * is available before each test case runs.
 */
trait DBTransactionPerTest extends BeforeAndAfterEach { this: Suite =>
  override def beforeEach() {
    super.beforeEach()
    SessionFactory.newSession.bindToCurrentThread
  }

}
