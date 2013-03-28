package controllers.website

import play.api.mvc._
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetSimpleMessageEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod

  def getSimpleMessage(header: String, body: String) = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      Ok(views.html.frontend.simple_message(header = header, body = body))
    }
  }
}