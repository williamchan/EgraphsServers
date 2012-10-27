package utils

import org.specs2.mock.Mockito
import play.api.mvc.Action
import services.http.POSTControllerMethod
import play.api.mvc.AnyContent
import services.AppConfig
import services.http.WithDBConnection
import services.http.WithoutDBConnection
import services.http.ControllerDBSettings
import services.http.ControllerMethod
import services.http.filters.RequireAuthenticityTokenFilter
import services.http.filters.RequireAuthenticityTokenFilterProvider
import services.db.DBSession

object Stubs extends Mockito {
  private def db = AppConfig.instance[DBSession]
  
  def postControllerMethod: POSTControllerMethod = {
    new POSTControllerMethod(mock[ControllerMethod], mock[RequireAuthenticityTokenFilterProvider]) {
      override def apply[A](
        doCsrfCheck: Boolean=true,
        dbSettings: ControllerDBSettings = WithDBConnection(readOnly = false)
      )(action: Action[A]): Action[A] = 
      {
        Action(action.parser) { request =>
          dbSettings match {
            case conn @ WithDBConnection(dbIsolation, readOnly) =>
              db.connected(dbIsolation)(action(request))
            case WithoutDBConnection =>
              action(request)
          }
        }
      }
    }
  }
}