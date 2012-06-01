package services.http

import com.google.inject.Inject
import java.util.Properties
import play.mvc.Http.Request
import play.mvc.results.Result

/**
 * Inspired by [play-framework] Forcing SSL connection:
 * https://groups.google.com/forum/?fromgroups#!searchin/play-framework/force$20ssl/play-framework/e5qja-hdD3s/8U2s-XfVkKwJ
 */
class HttpsFilter @Inject()(@PlayConfig playConfig: Properties) {

  def apply[A](operation: => A)
              (implicit request: Request): Either[Result, A] = {

    if (playConfig.getProperty(HttpsFilter.httpsOnlyProperty) == "true" && !request.secure.booleanValue()) {
      Left(new play.mvc.results.Redirect("https://" + request.host + request.url))
    }

    else Right(operation)
  }
}

object HttpsFilter {
  val httpsOnlyProperty = "application.httpsOnly"
}
