package services.http.filters

import com.google.inject.Inject
import play.api.mvc.{Results, Action}
import services.http.EgraphsSession.Conversions._


class RequireSessionUrlSlug @Inject() () {
  def apply[T](sessionSlug: String)(actionFactory: String => Action[T]) = {
    val producedAction = actionFactory(sessionSlug)
    Action[T](producedAction.parser) { implicit request =>
      request.session.id match {
        case Some(sessionSlug) => producedAction(request)
        case _ => Results.Forbidden
      }
    }
  }

}



