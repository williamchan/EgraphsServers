package db

import org.squeryl.SessionFactory

/**
 * Manages provision of Squeryl database sessions for every job or request.
 */
object DBSession {

  /**
   * Ensures that a Squeryl session is available for use in later calls. This
   * should be called before any application code in Controllers, Jobs, or Tests.
   * @see controllers.DBTransaction
   * @see test.utils.DBTransactionPerTest
   */
  def init() {
    // Make a new session
    val session = SessionFactory.newSession
    val conn = session.connection

    // Make sure it doesn't auto-commit each statement
    if (conn.getAutoCommit) {
      conn.setAutoCommit(false)
    }

    // Bind it to this thread for use
    session.bindToCurrentThread
  }

  def commit() {
    play.db.DB.getConnection.commit()
  }

  def rollback() {
    play.db.DB.getConnection.rollback()
  }
}