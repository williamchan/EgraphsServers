package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.CelebrityAccountRequestFilters

/**
 * Provides a single Celebrity's JSON representation for consumption by the API.
 */
private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrity = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      Serializer.SJSON.toJSON(celebrity.renderedForApi)
    }
  }
}
