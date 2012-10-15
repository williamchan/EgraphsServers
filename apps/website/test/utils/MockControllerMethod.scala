package utils

import services.http.{WithDBConnection, ControllerDBSettings, ControllerMethod}
import play.mvc.Http.Request

/**
 * Mocks out the interface of ControllerMethod, but does nothing except execute the operation
 */
object MockControllerMethod extends ControllerMethod(null, null, null) {
  override def apply[A](dbSettings: ControllerDBSettings = WithDBConnection())
                       (operation: => A)
                       (implicit request: Request): Any = {
    operation
  }
}
