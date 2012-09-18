package services.http

import play.api.mvc.Result
import play.api.mvc.Request
import models._
import com.google.inject.Inject
import controllers.WebsiteControllers
import play.api.mvc.Results.Redirect
import controllers.website.GetLoginEndpoint

class CustomerRequestFilters @Inject()(customerStore: CustomerStore) {

  import SafePlayParams.Conversions._

  def requireCustomerLogin(continue: (Customer, Account) => Result)(implicit request: Request[_]): Result = {
    val session = request.session
    val customerIdOption = session.get(EgraphsSession.Key.CustomerId.name).map(customerId => customerId.toLong)
    val customerOption = customerIdOption match {
      case None => None
      case Some(customerId) => customerStore.findById(customerId)
    }
    customerOption match {
      case None => {
        Redirect(GetLoginEndpoint.url().url).withNewSession
      }
      case Some(customer) => continue(customer, customer.account)
    }
  }
}
