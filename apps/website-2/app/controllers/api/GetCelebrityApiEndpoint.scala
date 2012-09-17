package controllers.api

import play.api.mvc.Controller
import sjson.json.Serializer
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Celebrities%C2%A0Celebrities the json spec]].
   */
  def getCelebrity = controllerMethod() {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      Serializer.SJSON.toJSON(celebrity.renderedForApi)
    }
  }
}
