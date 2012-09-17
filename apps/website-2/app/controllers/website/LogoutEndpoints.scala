package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import services.http.{ControllerMethod, POSTControllerMethod}

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = Action { request =>
    controllerMethod(openDatabase = false) {
      logout()
    }
  }

  def postLogout() = postController(openDatabase=false) {
    logout()
  }

  private def logout() = {
    val session = play.mvc.Http.Context .current().session()
    session.clear()
    Redirect("/")
  }
}
