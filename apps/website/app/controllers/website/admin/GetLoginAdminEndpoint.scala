package controllers.website.admin

import services.Utils
import services.http.ControllerMethod
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetLoginAdminEndpoint  extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  def getLoginAdmin = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      val flash = request.flash
      val errorFields = flash.get("errors").map(errString => errString.split(',').toList)
      Ok(views.html.Application.admin.admin_login(errorFields = errorFields))
    }
  }

}

object GetLoginAdminEndpoint {

  def url() = {
    controllers.routes.WebsiteControllers.getLoginAdmin.url
  }
}