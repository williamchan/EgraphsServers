package services.http

import play.mvc.Http.Request
import services.logging.LoggingContext
import com.google.inject.Inject
import services.db.{TransactionIsolation, TransactionSerializable, DBSession}

/**
 * High-level behavior specifier for a controller method. Every controller method
 * should begin with a call to this class.
 */
class ControllerMethod @Inject()(logging: LoggingContext, db: DBSession) {

  /**
   * Prepares and customizes controller method behavior. The first expression in
   * any controller methods in our codebase should be to call this function.
   *
   * @param openDatabase true that the a database connection should be managed
   *    by this ControllerMethod instance.
   * @param dbIsolation the transaction isolation with which to connect to the
   *    database if openDatabase is true
   * @param operation
   *    the code block to execute after setting up the connection resources
   * @param request
   *    the request being served
   *
   * @return the result of the `operation` code block.
   */
  def apply[A](openDatabase:Boolean=true,
               dbIsolation: TransactionIsolation = TransactionSerializable)
              (operation: => A)
              (implicit request: Request): A =
  {
    logging.withContext(request) {
      if (openDatabase) {
        db.connected(dbIsolation) {
          operation
        }
      }
      else {
        operation
      }
    }
  }
}
