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
  def apply[A](dbIsolation: TransactionIsolation = TransactionSerializable)
              (operation: => A)
              (implicit request: Request): A =
  {
    logging.withContext(request) {
      db.connected(dbIsolation) {
        operation
      }
    }
  }
}
