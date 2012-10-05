package services.http.filters

import java.util.UUID
import com.google.inject.Inject
import play.api.mvc.Request
import play.api.mvc.Action
import play.api.mvc.PlainResult
import services.http.ResultUtils.resultToRichResult
import play.api.mvc.WrappedRequest
import services.http.EgraphsSession

/**
 * This filter will ensure that there is a session id for the given request. It will create a new
 * session id if one doesn't already exist.
 */
class RequireSessionId @Inject() {
  def apply[A](action: Action[A]): Action[A] = {
    Action(action.parser) { request =>
      val (result, sessionId) = getResultAndSessionIdFromRequest(request, action)
      result match {
        case result: PlainResult => result.withSession(result.session + (EgraphsSession.SESSION_ID_KEY -> sessionId))
        case other => other
      }
    }
  }

  /**
   * Get the result and session id from the request.  If there is no session id in the request a
   * new session id will be created.
   */
  private def getResultAndSessionIdFromRequest[A](request: Request[A], action: Action[A]) = {
    request.session.get(EgraphsSession.SESSION_ID_KEY) match {
      case Some(sessionId) => {
        val result = action(request)
        (result, sessionId)
      }
      case None => {
        val sessionId = createNewSessionId
        val newRequest = new WrappedRequest(request) {
          override val session = request.session + (EgraphsSession.SESSION_ID_KEY -> sessionId)
        }
        val result = action(newRequest)
        (result, sessionId)
      }
    }
  }

  private def createNewSessionId = {
    UUID.randomUUID().toString()
  }
}


