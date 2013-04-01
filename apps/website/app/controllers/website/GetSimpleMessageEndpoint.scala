package controllers.website

import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetSimpleMessageEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  /**
   * Use this to display a simple, text-only page with a header and body.
   *
   * See PostRecoverAccountEndpoint for example usage.
   */
  def getSimpleMessage(header: String, body: String) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      Ok(views.html.frontend.simple_message(header = header, body = body))
    }
  }
}