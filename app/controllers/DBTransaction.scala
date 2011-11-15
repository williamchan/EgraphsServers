package controllers

import org.squeryl.{Session, SessionFactory}
import play.mvc.{Finally, Before}

/**
 * Ensures that a database transaction is available for use during any request.
 *
 * Play enforces that each request gets a transaction which commits after the request
 * or rolls back if an exception is caught.
 **/
trait DBTransaction {

  @Before(priority=5)
  def initSquerylTransaction() {
    SessionFactory.newSession.bindToCurrentThread
  }

}