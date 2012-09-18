package services.http

import play.mvc.{Before, Controller}
import models._
import play.api.mvc.Request
import com.google.inject.Inject
import play.mvc.results.{NotFound, Forbidden}

/**
 * Provides functions whose callback parameters are only called when the egraphs database
 * found Accounts that match provided credentials.
 */
class AccountRequestFilters @Inject() (accountStore: AccountStore, customerStore: CustomerStore) {

  /**
   * Calls the `continue` callback parameter only if the basic-auth user and password values
   * corresponded to the username and password of an [[models.Account]] in our database.
   *
   * @param continue callback that accepts the [[models.Account]] that corresponded to the
   *     provided username/password
   * @param request the current request which can be used to access basic auth parameters.
   *
   * @return either Forbidden or the result of `continue`.
   */
  def requireAuthenticatedAccount(continue: Account => Any)(implicit request: Request) = {
    accountStore.authenticate(request.user, request.password) match {
      case Right(theAccount) =>
        continue(theAccount)

      case Left(_: AccountAuthenticationError) =>
        new Forbidden("Email/password information was incorrect.")
    }
  }

  /**
   * Validate that customer exists
   */

  def requireValidCustomerId(customerId: Long)(continue: Customer => Any)(implicit request: Request) = {
    customerStore.findById(customerId) match {
      case Some(customer) => continue(customer)
      case _ => new NotFound("Customer not found.")
    }
  }

  def requireValidCustomerUsername(username: String)(continue: Customer => Any)(implicit request: Request) = {
    customerStore.findByUsername(username) match {
      case Some(customer) =>  continue(customer)
      case _ => new NotFound("Customer not found.")
    }
  }

  def requireValidAccountEmail(email:String)(continue: Account => Any)(implicit request: Request) = {
    accountStore.findByEmail(email) match {
      case Some(account) => continue(account)
      case _ => new NotFound("Account not found.")
    }
  }

}