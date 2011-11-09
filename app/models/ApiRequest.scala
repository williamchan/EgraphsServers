package models

import play.mvc.Http.Request

class ApiRequest(request: Request) {
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