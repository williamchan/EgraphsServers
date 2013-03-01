package services.http.filters

import com.google.inject.Inject
import play.api.mvc.{BodyParser, Results, Action}
import services.http.EgraphsSession.Conversions._
import play.api.mvc.BodyParsers.parse

class RequireSessionUrlSlug @Inject() () {
  def apply[T](sessionId: String, parser: BodyParser[T] = parse.anyContent)(actionFactory: String => Action[T]) = {
    Action(parser) { implicit request =>
      request.session.id match {
        case Some(sessionId) if sessionId == sessionId => actionFactory(sessionId)(request)
        case _ => Results.Forbidden
      }
    }
  }
}
