package services.http.filters

import com.google.inject.Inject

import models.Customer
import models.CustomerStore
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

/**
 * Filter for requiring a customer username with an customer existing in the customer store to be provided.
 */
class RequireCustomerUsername @Inject() (customerStore: CustomerStore) extends Filter[String, Customer] {

  override def filter(username: String): Either[Result, Customer] = {
    val maybeCustomerAccount = customerStore.findByUsername(username).map(customer => customer)
    maybeCustomerAccount.toRight(left = NotFound("Customer not found"))
  }
}
