package controllers.website

import play.mvc.Controller
import services.AppConfig
import services.logging.LoggingContext

private[controllers] trait GetRootEndpoint { this: Controller =>
  import views.Application._

  val logging = AppConfig.instance[LoggingContext]

  /**
   * Serves the application's landing page.
   */
  def getRootEndpoint = {
    logging.withContext(request) {
      html.index()
    }
  }
}