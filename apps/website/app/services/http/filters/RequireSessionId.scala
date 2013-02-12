package services.http.filters

import java.util.UUID
import com.google.inject.Inject
import play.api.mvc.Action
import egraphs.playutils.ResultUtils.RichResult
import play.api.mvc.WrappedRequest
import services.http.EgraphsSession
import services.http.EgraphsSession.Conversions._

/**
 * This filter will ensure that there is a session id for the given request. It will create a new
 * session id if one doesn't already exist.
 */
class RequireSessionId @Inject() {
  def apply[A](action: Action[A]): Action[A] = {
    
    Action(action.parser) { implicit request =>
      request.session.id match {
        case Some(sessionId) => 
	        action(request)
	        
        case None =>
          val sessionIdCookiePair = (EgraphsSession.SESSION_ID_KEY -> createNewSessionId)
          val newRequest = new WrappedRequest(request) {
            override lazy val session = request.session + sessionIdCookiePair
          }
          val result = action(newRequest)
          result.addingToSession(sessionIdCookiePair)
      }
    }
  }

  private def createNewSessionId = {
    UUID.randomUUID().toString()
  }
}


