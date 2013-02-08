package utils

import services.http.{WithDBConnection, ControllerDBSettings, ControllerMethod}
import play.api.mvc.Action

/**
 * Mocks out the interface of ControllerMethod, but does nothing except execute the operation
 */
object MockControllerMethod extends ControllerMethod(null, null, null, null, null) {
  
  override def apply[A](dbSettings: ControllerDBSettings = WithDBConnection())
              (action: Action[A]): Action[A] =
  {
    action
  }
}
