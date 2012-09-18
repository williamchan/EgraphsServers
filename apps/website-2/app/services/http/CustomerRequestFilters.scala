package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import controllers.WebsiteControllers
import play.api.mvc.Results.Redirect
import controllers.website.GetLoginEndpoint

class CustomerRequestFilters @Inject()(customerStore: CustomerStore) {

  import SafePlayParams.Conversions._

  def requireCustomerLogin(continue: (Customer, Account) => Any)(implicit request: Request) = {
    val customerIdOption = session.getLongOption(WebsiteControllers.customerIdKey)
    val customerOption = customerIdOption match {
      case None => None
      case Some(customerId) => customerStore.findById(customerId)
    }
    customerOption match {
      case None => {
        session.clear()
        new Redirect(GetLoginEndpoint.url().url)
      }
      case Some(customer) => continue(customer, customer.account)
    }
  }
}
