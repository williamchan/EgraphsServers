package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import services.http.{ControllerMethod, POSTControllerMethod}
import play.api.mvc.Result

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = Action { implicit request =>
    controllerMethod(openDatabase = false) {
      logout()
    }
  }

  def postLogout() = Action { implicit request => 
    postController(openDatabase=false) {
      logout()
    }
  }

  private def logout(): Result = {
    Redirect("/").withNewSession
  }
}
