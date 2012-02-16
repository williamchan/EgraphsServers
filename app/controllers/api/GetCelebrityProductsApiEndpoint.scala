package controllers.api

import services.http.CelebrityAccountRequestFilters
import sjson.json.Serializer
import play.mvc.Controller

/**
 * Returns the list of a Celebrity's products given a request that successfully authenticates as
 * a Celebrity, returns a list of that Celebrity's products.
 */
private[controllers] trait GetCelebrityProductsApiEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityProducts = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      val productMaps = celebrity.products().map(product => product.renderedForApi)
      Serializer.SJSON.toJSON(productMaps)
    }
  }
}