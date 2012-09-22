package controllers.api

import sjson.json.Serializer
import play.api.mvc.Controller
import services.http.{ ControllerMethod, CelebrityAccountRequestFilters }
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId

private[controllers] trait GetCelebrityProductsApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def requireAuthenticatedAccount: RequireAuthenticatedAccount
  protected def requireCelebrityId: RequireCelebrityId

  /**
   * Returns a JSON array of a Celebrity's Products.
   *
   * See the [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Products%C2%A0Products product json spec]]
   */
  def getCelebrityProducts = controllerMethod() {
    requireAuthenticatedAccount() { accountRequest =>
      val action = requireCelebrityId.inAccount(accountRequest.account) { celebrityRequest =>
        val productMaps = celebrityRequest.celeb.products().map(product => product.renderedForApi)
        Ok(Serializer.SJSON.toJSON(productMaps))
      }

      action(accountRequest)
    }
  }
}