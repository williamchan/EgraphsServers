package services.http

import com.google.inject.Inject
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request}
import play.api.mvc.Action
import play.api.mvc.Session
import services.config.ConfigFileProxy

/**
 * Inspired by [play-framework] thread:
 * https://groups.google.com/forum/#!topic/play-framework/11zbMtNI3A8
 */
class HttpsFilter @Inject()(config: ConfigFileProxy) {

  def apply[A](action: Action[A]): Action[A] = {
    Action(action.parser) { request =>
      if (config.applicationHttpsOnly && (request.headers.get("x-forwarded-proto").getOrElse("") != "https")) {
        Redirect("https://" + request.host + request.uri)
      } else {
        action(request)
      }
    }
  }
}
