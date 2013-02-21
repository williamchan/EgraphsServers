package controllers.api

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import models.JsProduct

private[controllers] trait GetCelebrityProductsApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  /**
   * Returns a JSON array of a Celebrity's Products.
   *
   * See the [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Products%C2%A0Products product json spec]]
   */
  def getCelebrityProducts = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celeb =>
        Action {
          Ok(Json.toJson(celeb.products().map(JsProduct.from(_))))
        }
      }
    }
  }
}