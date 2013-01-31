package services.http

import com.google.inject.Inject
import play.api.mvc.Action
import play.api.mvc.WrappedRequest
import play.api.http.Status
import services.http.EgraphsSession.Conversions._
import egraphs.playutils.RichResult.resultToRichResult

class AnalyticsActions @Inject() {
  def apply[A](action: Action[A]): Action[A] = {

    Action(action.parser) { implicit request =>
      request.session.isUsernameChanged match {
        case None => action(request)

        case Some(isUsernameChanged) =>
          val result = action(request)

          // if it has changed, it js needs to be run and will be triggered by ImplicitHeaderAndFooter code
          val maybeModifiedResult = for { status <- result.status if (status == Status.OK) } yield {
            // assume the alias will have changed, we don't want to do this again
            result.removeFromSession(EgraphsSession.Key.UsernameChanged.name)
          }
          maybeModifiedResult.getOrElse(result)
      }
    }
  }
}