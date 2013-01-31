package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import play.api.mvc.Request
import services.http.{ControllerMethod, POSTControllerMethod, WithoutDBConnection}
import services.http.EgraphsSession.Conversions._

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = controllerMethod(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      logout()
    }
  }

  def postLogout() = postController(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      logout()
    }
  }

  private def logout[A]()(implicit request: Request[A]): Result = {
    Redirect(controllers.routes.WebsiteControllers.getRootConsumerEndpoint)
      .withSession(request.session.removedCustomerId.removedAdminId)
  }
}
