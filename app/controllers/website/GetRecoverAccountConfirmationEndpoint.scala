package controllers.website

import play.mvc.Controller
import services.http.ControllerMethod

private[controllers] trait GetRecoverAccountConfirmationEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  def getRecoverAccountConfirmation = controllerMethod() {
    val emailOption = Option(flash.get("email"))
    emailOption match {
      case None => Forbidden("Page Expired.")
      case Some(email) => views.Application.html.recover_account_confirmation(email)
    }
  }

}