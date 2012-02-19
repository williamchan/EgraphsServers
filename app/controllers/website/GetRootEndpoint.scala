package controllers.website

import play.mvc.Controller

private[controllers] trait GetRootEndpoint { this: Controller =>
  import views.Application._

  /**
   * Serves the application's landing page.
   */
  def getRootEndpoint = {
    html.index()
  }
}