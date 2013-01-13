package controllers.api

import models._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import sjson.json.Serializer

private[controllers] trait GetCustomerEgraphsApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def orderStore: OrderStore
  protected def httpFilters: HttpFilters

  def getCustomerEgraphs = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCustomerId.inAccount(account) { customer =>
        Action {
          val egraphBundles: Iterable[FulfilledOrderBundle] = orderStore.findFulfilledForCustomer(customer)
          Ok(Serializer.SJSON.toJSON(egraphBundles.map(_.renderedForApi)))
        }
      }
    }
  }
}
