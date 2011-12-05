package controllers

import play.mvc.{Finally, Before}
import db.DBSession

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
    if (e == null) {
      DBSession.commit()
    } else {
      DBSession.rollback()
    }
  }

}