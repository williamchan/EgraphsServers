package services.db

import com.google.inject.Inject
import java.sql.{SQLException, Connection}
import play.exceptions.DatabaseException
import org.squeryl.{Session, SessionFactory}
import services.Logging

/**
 * Provides methods for performing calls against a database connection at a particular
 * Transaction isolation level.
 *
 * @param connectionFactory function that provides a Connection.
 */
class DBSession @Inject() (connectionFactory: () => Connection) extends Logging {
  import org.squeryl.PrimitiveTypeMode._

  /**
   * Executes `continue` within a database transaction at the specified isolation level. Closes the transaction
   * and returns the connection to the pool (by closing it) before returning.
   *
   * @param isolation Transaction isolation level to use for this connection.
   * @param continue code to execute when a connection is made with the specified isolation level
   * @tparam T the return value of `continue`
   * @return the return value of `continue`, after having closed the transaction and returned the connection
   *   to the pool.
   */
  def connected[T](isolation: TransactionIsolation)(continue: => T): T = {
    log("Connecting to a DB Connection from the pool at isolation level: " + isolation)
    val connection = connect(isolation)
    val squerylSession = new Session(connection, DBAdapter.current)

    try {
      log("Doing provided work with new connection")
      val result = using(squerylSession)(continue)

      log("Committing changes to the DB")
      connection.commit()

      result
    }
    catch {
      case e: Exception =>
        log("Rolling back changes to the DB due to a " + e.getClass.getName)
        connection.rollback()
        throw e
    }
    finally {
      log("Returning DB Connection to the pool")
      connection.close()
    }
  }

  /**
   * Retrieves a Connection from the datasource, which should be pooled. Formats and reports any errors.
   *
   * @return a connection from the datasource.
   */
  private def connect(isolation: TransactionIsolation): Connection = {
    try {
      val connection = connectionFactory()

      // Configure the connection: (1) no auto-commit, (2) correct transaction isolation.
      if (connection.getAutoCommit) {
        connection.setAutoCommit(false)
      }

      if (connection.getTransactionIsolation != isolation.jdbcIsolationLevel) {
        connection.setTransactionIsolation(isolation.jdbcIsolationLevel)
      }
      
      connection
    }
    catch {
      case sqlE: SQLException => {
        val msg = "Failed to obtain a new connection (" + sqlE.getMessage + ")"
        log(msg)
        throw new DatabaseException(msg, sqlE)
      }
    }
  }
}

/**
 * Manages provision of Squeryl database sessions for every job or request.
 */
object DBSession {

  /**
   * Ensures that a Squeryl session is available for use in later calls. This
   * should be called before any application code in Controllers or Tests. In general
   * you should not use this method where it is possible to use Squeryl's
   * `inTransaction` or `transaction` methods.
   *
   * @see services.http.DBTransaction
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

/**
 * Abstractions over the int-based Java Transaction Isolation enums. Read
 * see [[http://www.postgresql.org/docs/9.1/static/transaction-iso.html for more info]]
 **/
sealed abstract class TransactionIsolation(val jdbcIsolationLevel: Int)

case object TransactionNone extends TransactionIsolation(Connection.TRANSACTION_NONE)
case object TransactionReadCommitted extends TransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
case object TransactionRepeatableRead extends TransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
case object TransactionSerializable extends TransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
