package controllers

import org.squeryl.{Session, SessionFactory}
import play.mvc.{Finally, Before}
import db.DBSession

/**
 * Ensures that a database transaction is available for use during any request.
 *
 * Play only enforces commit-per-request when you have registerd JPA models.
 **/
trait DBTransaction {

  @Before(priority=5)
  def initSquerylTransaction() {
    DBSession.init()
  }

  @Finally
  def cleanUpPersistence(e: Throwable) {
    if (e == null) {
      DBSession.commit()
    } else {
      DBSession.rollback()
    }
  }

}