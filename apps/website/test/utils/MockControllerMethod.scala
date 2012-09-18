package utils

import services.http.ControllerMethod
import play.mvc.Http.Request
import services.db.{TransactionSerializable, TransactionIsolation}

/**
 * Mocks out the interface of ControllerMethod, but does nothing except execute the operation
 */
object MockControllerMethod extends ControllerMethod(null, null, null) {
  override def apply[A](openDatabase: Boolean = true,
                        dbIsolation: TransactionIsolation = TransactionSerializable,
                        readOnly: Boolean = false)
                       (operation: => A)
                       (implicit request: Request): Any = {
    operation
  }
}
