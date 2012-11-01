package services.http.filters

import com.google.inject.Inject

import models.Account
import models.Customer
import models.CustomerStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.Redirect
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

  private val redirectToLogin = Redirect(controllers.website.GetLoginEndpoint.url())

  override protected def badRequest(formWithErrors: Form[Long]): Result = redirectToLogin

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
