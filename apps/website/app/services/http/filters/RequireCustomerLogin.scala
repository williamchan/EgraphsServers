package services.http.filters

import com.google.inject.Inject

import models.Account
import models.Customer
import models.CustomerStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.Redirect
import play.api.mvc.Request
import play.api.mvc.Result
import services.http.filters.RequireCustomerLogin.CustomerAccount
import services.http.EgraphsSession

object RequireCustomerLogin {
  type CustomerAccount = (Customer, Account)
}

/**
 * Filter for requiring a customer with an account to be provided.
 */
class RequireCustomerLogin @Inject() (customerStore: CustomerStore) extends Filter[Long, CustomerAccount] with RequestFilter[Long, CustomerAccount] {


  private val redirectToLogin = Redirect(controllers.routes.WebsiteControllers.getLogin)

  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    source match {
      case SessionSource => redirectToLogin.withSession(request.session - EgraphsSession.Key.CustomerId.name)
      case _ => redirectToLogin
    }
  }

  override def filter(customerId: Long): Either[Result, CustomerAccount] = {
    val maybeCustomerAccount = customerStore.findById(customerId).map(customer => (customer, customer.account))
    maybeCustomerAccount.toRight(left = redirectToLogin)
  }

  override val form: Form[Long] = Form(
    single(
      EgraphsSession.Key.CustomerId.name -> longNumber)
      verifying ("Invalid customerId", {
        case customerId => customerId > 0
      }: Long => Boolean))
}
