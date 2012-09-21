package services.http

import com.google.inject.Inject
import java.util.Properties
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request}
import play.api.mvc.Action
import play.api.mvc.Session

/**
 * Inspired by [play-framework] Forcing SSL connection:
 * https://groups.google.com/forum/?fromgroups#!searchin/play-framework/force$20ssl/play-framework/e5qja-hdD3s/8U2s-XfVkKwJ
 */
class HttpsFilter @Inject()(@PlayConfig playConfig: Properties) {

  def apply[A](action: Action[A]): Action[A] = {
    Action(action.parser) { request =>
      // TODO: PLAY20 migration. Fix this broken implementation -- right now there is no known way to
      //   actually redirect https. http://stackoverflow.com/questions/12522390/how-do-i-find-out-if-my-request-was-made-over-http-or-https-in-play-2-0
      action(request)
      /* Uncomment this once we know how to reliably perform this redirect.
        if (playConfig.getProperty(HttpsFilter.httpsOnlyProperty) == "true" && !request.secure.booleanValue()) {
        Redirect("https://" + request.host + request.uri)
      } else {
        action(request)
      }*/
    }
  }
}

object HttpsFilter {
  val httpsOnlyProperty = "application.httpsOnly"
}
