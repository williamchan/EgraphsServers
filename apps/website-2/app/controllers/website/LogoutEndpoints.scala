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

  def getLogout() = controllerMethod(openDatabase = false) {
    Action {  
      logout()
    }
  }

  def postLogout() = postController(openDatabase=false) {
    Action {
      logout()
    }
  }

  private def logout(): Result = {
    Redirect("/").withNewSession
  }
}
