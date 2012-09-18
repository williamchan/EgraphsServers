package services.http

import models._
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.{NotFound, Forbidden}
import play.api.mvc.Security
import com.google.inject.Inject

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
  def requireAuthenticatedAccount(continue: Account => Result)(implicit request: Request[_]): Result = {
    //TODO: PLAY20 migration: maybe we should do something more like this: http://www.playframework.org/documentation/2.0.3/ScalaSecurity
    val maybeUsername = request.session.get(Security.username)
    val maybePassword = request.session.get("password")

    if (maybeUsername.isEmpty || maybePassword.isEmpty ) {
      Forbidden("Username or password information was incorrect.")
    } else {
      val username = maybeUsername.get
      val password = maybePassword.get

      accountStore.authenticate(username, password) match {
        case Right(theAccount) =>
          continue(theAccount)
  
        case Left(_: AccountAuthenticationError) =>
          Forbidden("Email/password information was incorrect.")
      }
    }
  }

  /**
   * Validate that customer exists
   */

  def requireValidCustomerId(customerId: Long)(continue: Customer => Result)(implicit request: Request[_]): Result = {
    customerStore.findById(customerId) match {
      case Some(customer) => continue(customer)
      case _ => NotFound("Customer not found.")
    }
  }

  def requireValidCustomerUsername(username: String)(continue: Customer => Result)(implicit request: Request[_]): Result = {
    customerStore.findByUsername(username) match {
      case Some(customer) =>  continue(customer)
      case _ => NotFound("Customer not found.")
    }
  }

  def requireValidAccountEmail(email:String)(continue: Account => Result)(implicit request: Request[_]): Result = {
    accountStore.findByEmail(email) match {
      case Some(account) => continue(account)
      case _ => NotFound("Account not found.")
    }
  }

}