package services.http

import com.google.inject.Inject
import play.api.mvc.Action
import play.api.mvc.WrappedRequest
import play.api.http.Status
import services.http.EgraphsSession.Conversions._
import egraphs.playutils.ResultUtils.RichResult

class AnalyticsActions @Inject() {

  /**
   * Wraps the input action. If we had previously changed the username and
   * redirected the client, then when they come back we will trigger an
   * event in the js for when the username is updated. Finally, removing
   * the username changed cookie from their session.
   * 
   * We won't accidentally remove the cookie we just added to signal the username
   * changed so long as we set that cookie in a redirect and not an 200 ok.
   */
  def apply[A](action: Action[A]): Action[A] = {

    Action(action.parser) { implicit request =>
      request.session.isUsernameChanged match {
        case None => action(request)

        case Some(isUsernameChanged) =>
          val result = action(request)

          // if it has changed, js needs to be run and will be triggered by ImplicitHeaderAndFooter code
          val maybeModifiedResult = for { status <- result.status if (status == Status.OK) } yield {
            // assume the alias will have changed, we don't want to do this again
            result.removeFromSession(EgraphsSession.Key.UsernameChanged.name)
          }
          maybeModifiedResult.getOrElse(result)
      }
    }
  }
}