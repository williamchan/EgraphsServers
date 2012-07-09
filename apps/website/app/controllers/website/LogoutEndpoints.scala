package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.http.{ControllerMethod, POSTControllerMethod}

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = controllerMethod(openDatabase=false) {
    logout()
  }

  def postLogout() = postController(openDatabase=false) {
    logout()
  }

  private def logout(): Redirect = {
    session.clear()
    new Redirect("/")
  }
}
