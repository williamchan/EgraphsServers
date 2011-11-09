package controllers

import play.mvc.{Before, Controller}
import models._


trait RequiresAuthenticatedAccount { this: Controller =>
  import models.ApiRequest.Conversions._

  private val _account = new ThreadLocal[Account]

  def account:Account = {
    _account.get
  }

  @Before(priority=10)
  def ensureRequestAuthenticated = {
    request.authenticatedAccount match {
      case Right(theAccount) =>
        _account.set(theAccount)
        Continue
        
      case Left(_: AccountAuthenticationError) =>
        Forbidden("Login/password information was incorrect.")
    }
  }
}
