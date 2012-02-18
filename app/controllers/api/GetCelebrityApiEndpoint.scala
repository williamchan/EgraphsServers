package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.CelebrityAccountRequestFilters

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Provides a single Celebrity's JSON representation for consumption by the API.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Celebrities%C2%A0Celebrities the json spec]].
   */
  def getCelebrity = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      Serializer.SJSON.toJSON(celebrity.renderedForApi)
    }
  }
}
