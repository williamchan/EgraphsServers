package controllers.api

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import models.JsCelebrity

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Celebrities%C2%A0Celebrities the json spec]].
   */
  def getCelebrity = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          Ok(Json.toJson(JsCelebrity.from(celeb)))
        }
      }
    }
  }
}