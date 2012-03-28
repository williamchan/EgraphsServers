package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod

private[controllers] trait GetRootEndpoint { this: Controller =>
  import views.Application._

  protected def controllerMethod: ControllerMethod

  /**
   * Serves the application's landing page.
   */
  def getRootEndpoint = controllerMethod() {
    html.index()
  }
}