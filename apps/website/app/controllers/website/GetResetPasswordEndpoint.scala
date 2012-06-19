package controllers.website

import play.mvc.Controller
import services.Utils
import services.http.ControllerMethod
import models.AccountStore
import play.mvc.Router.ActionDefinition

private[controllers] trait GetResetPasswordEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def accountStore: AccountStore

  def getResetPassword(accountId: Long, passwordRecoveryKey: String) = controllerMethod() {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    accountStore.findById(accountId) match {
      case Some(account) if account.verifyResetPasswordKey(passwordRecoveryKey) =>
        val fieldDefaults: (String => String) = {
          (paramName: String) => paramName match {
            case "email" => account.email
            case "displayemail" => account.email // non-editable form fields don't seem to be posted, so using displayemail
                                                 // for display and using email as a hidden form field
            case _ =>
              Option(flash.get(paramName)).getOrElse("")
          }
        }

        views.Application.html.reset_password(errorFields = errorFields, fields = fieldDefaults)

      case _ =>
        Forbidden("The password recovery URL you used is either out of date or invalid.")
    }
  }

}

object GetResetPasswordEndpoint {

  def url(accountId: Long, resetPasswordKey: String): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getResetPassword",
      Map("accountId" -> accountId.toString, "passwordRecoveryKey" -> resetPasswordKey))
  }
}