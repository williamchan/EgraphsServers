package controllers.website

import services.Utils
import play.mvc.Router.ActionDefinition
import models.{CustomerStore, AccountStore}
import services.http.ControllerMethod
import play.mvc.Controller
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetVerifyAccountEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def customerStore: CustomerStore
  protected def accountStore: AccountStore

  def getVerifyAccount(accountId: String, key: String) = controllerMethod() {

    accountStore.findByCustomerId(accountId.toLong) match {
      case Some(account) if account.verifyResetPasswordKey(key) == true =>
        account.emailVerify().save()

        val customerOption = for(customerId <- account.customerId;
            customerOption   <- customerStore.findById(customerId)) yield {
          customerOption
        }
        //User should always have both of these
        val (username, name) = customerOption match {
          case Some(customer) => (customer.username, customer.name)
          case None => ("", "")
        }
        views.frontend.html.account_verification(username, name)
      case _ =>
        Forbidden("The verification URL you used is either out of date or invalid.")
    }
  }
}

object GetVerifyAccountEndpoint {
  def url(customerId: Long, key: String ) :String = {
    "/account/verify?accountId=" + customerId +
      "&key=" + key
  }
}


