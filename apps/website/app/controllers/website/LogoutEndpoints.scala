package controllers.website

import play.mvc.Controller
import play.mvc.results.Redirect
import services.http._

private[controllers] trait LogoutEndpoints {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def postController: POSTControllerMethod

  def getLogout() = controllerMethod(dbSettings = WithoutDBConnection) {
    logout()
  }

  def postLogout() = postController(dbSettings = WithoutDBConnection) {
    logout()
  }

  private def logout(): Redirect = {
    session.clear()
    new Redirect("/")
  }
}
