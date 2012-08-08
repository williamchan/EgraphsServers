package services.db

import com.google.inject.Inject
import java.sql.{SQLException, Connection}
import play.exceptions.DatabaseException
import org.squeryl.{Session, SessionFactory}
import services.logging.Logging

/**
 * Provides methods for performing calls against a database connection at a particular
 * Transaction isolation level.
 *
 * @param connectionFactory function that provides a Connection.
 */
class DBSession @Inject() (connectionFactory: () => Connection) extends Logging {
  import org.squeryl.PrimitiveTypeMode._

  // Reference: http://www.postgresql.org/docs/9.1/static/transaction-iso.html
  private val errorStr40001 = "ERROR: could not serialize access due to read/write dependencies among transactions"

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
  def connected[T](isolation: TransactionIsolation, numTries: Int = 25)(continue: => T): T = {
    log("Connecting to a DB Connection from the pool at isolation level: " + isolation)
    val connection = connect(isolation)
    val squerylSession = new Session(connection, DBAdapter.current)

    try {
      for (i <- 1 until numTries) {
        try {
          return attemptTransaction(squerylSession, continue, connection)
        }
        catch {
          // This is rarely the case because Squeryl wraps PSQLExceptions into RuntimeExceptions!
          // case e: PSQLException if e.getSQLState == sqlState_SerializableConcurrentException => {
          //   println("retrying due to sqlState " + sqlState_SerializableConcurrentException)
          // }
          case e: Exception if (Option(e.getCause).isDefined && e.getCause.getMessage.startsWith(errorStr40001)) => {
            // rollback transaction and allowing subsequent retries to execute
            rollbackAndContinue(connection)
          }
          case e: Exception =>
            // rollback transaction and throwing exception again to exit method
            rollbackAndThrow(e, connection)
        }
      }

      // final try
      try {
        attemptTransaction(squerylSession, continue, connection)
      }
      catch {
        case e: Exception =>
          rollbackAndThrow(e, connection)
      }

    } finally {
      log("Returning DB Connection to the pool")
      connection.close()
    }
  }

  private def attemptTransaction[T](squerylSession: Session, continue: => T, connection: Connection): T = {
    val result = using(squerylSession)(continue)
    log("Committing changes to the DB")
    connection.commit()
    result
  }

  private def rollbackAndThrow[T](e: scala.Exception, connection: Connection): Nothing = {
    log("Rolling back changes to the DB due to a " + e.getClass.getName)
    connection.rollback()
    throw e
  }

  private def rollbackAndContinue[T](connection: Connection) {
    log("Rolling back changes to the DB and retrying")
    connection.rollback()
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
   * should be before any application code in Controllers or Tests. In general
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
