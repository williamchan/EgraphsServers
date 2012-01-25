package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.{DBTransaction, CelebrityAccountRequestFilters}

private[controllers] trait GetCelebrityApiEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrity = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      Serializer.SJSON.toJSON(celebrity.renderedForApi)
    }
  }
}


/**
 * Controllers that handle direct API requests for celebrity resources.
 */

