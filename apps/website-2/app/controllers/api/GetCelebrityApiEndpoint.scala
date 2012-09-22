package controllers.api

import play.api.mvc._
import sjson.json.Serializer
import services.http.{ ControllerMethod, CelebrityAccountRequestFilters }
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def requireAuthenticatedAccount: RequireAuthenticatedAccount
  protected def requireCelebrityId: RequireCelebrityId

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Celebrities%C2%A0Celebrities the json spec]].
   */
  def getCelebrity = controllerMethod() {
    requireAuthenticatedAccount() { accountRequest =>
      val action = requireCelebrityId.inAccount(accountRequest.account) { celebrityRequest =>
        Ok(Serializer.SJSON.toJSON(celebrityRequest.celeb.renderedForApi))
      }

      action(accountRequest)
    }
  }
}