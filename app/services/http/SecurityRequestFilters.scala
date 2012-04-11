package services.http

import com.google.inject.Inject
import play.mvc.Scope.Session
import play.mvc.results.Forbidden
import play.mvc.Http.Request
import play.Play

class SecurityRequestFilters @Inject()() {

  def checkAuthenticity(continue: => Any)(implicit session: Session, request: Request) = {
    val params = request.params
    val isTestMode = (Play.id == "test")
    if (isTestMode && params.get("authenticityCheck") != "fail") {
      continue
    } else if (params.get("authenticityToken") == null || !params.get("authenticityToken").equals(session.getAuthenticityToken)) {
      new Forbidden("Bad authenticity token")
    } else {
      continue
    }
  }
}
