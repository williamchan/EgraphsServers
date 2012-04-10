package services.http

import play.mvc.Scope.Session
import play.mvc.results.Forbidden
import play.mvc.Http.Request
import com.google.inject.Inject
import play.Play
import utils.TestData

class SecurityRequestFilters @Inject()() {

  def checkAuthenticity(continue: => Any)(implicit session: Session, request: Request) = {
    val params = request.params
    println("Play.id " + Play.id)
    if (!TestData.passAuthenticityCheck && (params.get("authenticityToken") == null || !params.get("authenticityToken").equals(session.getAuthenticityToken))) {
      new Forbidden("Bad authenticity token")
    } else {
      continue
    }
  }
}
