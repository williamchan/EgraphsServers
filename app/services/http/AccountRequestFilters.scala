package services.http

import play.mvc.{Before, Controller}
import models._
import play.mvc.Http.Request
import com.google.inject.Inject
import play.mvc.results.Forbidden

// TODO(erem): Test this class.
class AccountRequestFilters @Inject() (accountStore: AccountStore) {

  //
  // Filter methods
  //
  def requireAuthenticatedAccount(onAllow: Account => Any)(implicit request: Request) = {
    accountStore.authenticate(request.user, request.password) match {
      case Right(theAccount) =>
        onAllow(theAccount)

      case Left(_: AccountAuthenticationError) =>
        new Forbidden("Email/password information was incorrect.")
    }
  }
}