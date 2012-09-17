package controllers.api

import sjson.json.Serializer
import play.api.mvc.Controller
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}

private[controllers] trait GetCelebrityProductsApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Returns a JSON array of a Celebrity's Products.
   *
   * See the [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Products%C2%A0Products product json spec]]
   */
  def getCelebrityProducts = controllerMethod() {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      val productMaps = celebrity.products().map(product => product.renderedForApi)
      Serializer.SJSON.toJSON(productMaps)
    }
  }
}