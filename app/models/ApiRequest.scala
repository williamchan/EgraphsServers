package models

import play.mvc.Http.Request

/**
 * Pimped {{play.mvc.Http.Request}} object. Provides access to the account
 * authenticated by the credentials provided in the request.
 */
class ApiRequest(request: Request) {
  /**
   * Returns either the authenticated account (right) or the reason for
   * failure to authenticate (left).
   */
  def authenticatedAccount: Either[AccountAuthenticationError, Account] = {
    Account.authenticate(request.user, request.password)
  }
}

object ApiRequest {
  object Conversions {
    implicit def requestToApiRequest(request: Request): ApiRequest = {
      new ApiRequest(request)
    }
  }
}