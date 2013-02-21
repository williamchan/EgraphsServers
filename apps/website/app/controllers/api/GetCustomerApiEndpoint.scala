package controllers.api

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters

// TODO(egraph-exploration): Work in progress. Not finalized. Used for rapid prototyping.
private[controllers] trait GetCustomerApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCustomer = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCustomerId.inAccount(account) { customer =>
        Action {
          Ok(Json.toJson(customer))
        }
      }
    }
  }
}