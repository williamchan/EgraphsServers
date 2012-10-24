package controllers.website

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import services.http.{ControllerMethod, POSTControllerMethod, WithoutDBConnection}
import play.api.mvc.Result

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = controllerMethod(dbSettings = WithoutDBConnection) {
    Action {  
      logout()
    }
  }

  def postLogout() = postController(dbSettings = WithoutDBConnection) {
    Action {  
      logout()
    }
  }

  private def logout(): Result = {
    Redirect("/").withNewSession
  }
}
