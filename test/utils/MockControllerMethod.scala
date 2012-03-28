package utils

import services.http.ControllerMethod
import services.db.TransactionIsolation
import play.mvc.Http.Request

/**
 * Mocks out the interface of ControllerMethod, but does nothing except execute the operation
 */
object MockControllerMethod extends ControllerMethod(null, null) {
  override def apply[A](dbIsolation: TransactionIsolation)(operation: => A)(implicit request: Request): A = {
    operation
  }
}
