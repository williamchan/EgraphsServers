package services.http

import play.mvc.{Finally, Before}
import services.db.DBSession

/**
 * Ensures that a database transaction is available for use during any request.
 *
 * Play only provides automatic commit-per-request and rollback handling when you have registered JPA models.
 * To avoid invocation of JPA code, we use DBTransaction to manage interaction between requests and DBSession.
 **/
trait DBTransaction {

  @Before(priority = 5)
  def initSquerylTransaction() {
    DBSession.init()
  }

  @Finally
  def cleanUpPersistence(e: Throwable) {
    e match {
      case null => DBSession.commit()
      case exception => DBSession.rollback()
    }
  }

}