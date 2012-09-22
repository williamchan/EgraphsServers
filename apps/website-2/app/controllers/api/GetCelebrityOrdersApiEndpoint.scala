package controllers.api

import models.OrderQueryFilters
import models.OrderStore
import play.api.mvc.Controller
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId
import services.http.ControllerMethod
import sjson.json.Serializer

private[controllers] trait GetCelebrityOrdersApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters
  protected def requireAuthenticatedAccount: RequireAuthenticatedAccount
  protected def requireCelebrityId: RequireCelebrityId

  /**
   * Provides a JSON array of a celebrity's Orders for consumption
   * by the API. See the
   * [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Orders%C2%A0Orders JSON specification]]
   *
   * @param signerActionable true if the only Orders that should
   *   be returned are the ones that the celebrity needs to sign.
   */
  def getCelebrityOrders(signerActionable: Option[Boolean]) = controllerMethod() {
    requireAuthenticatedAccount() { accountRequest =>
      val action = requireCelebrityId.inAccount(accountRequest.account) { celebrityRequest =>
        signerActionable match {
          case None | Some(false) =>
            //TODO: PLAY20: Maybe this shouldn't be an InternalServerError, but I don't know how this would change the device code if it changed.
            InternalServerError("Please pass in signerActionable=true")

          case _ => {
            val orders = orderStore.findByCelebrity(celebrityRequest.celeb.id, orderQueryFilters.actionableOnly: _*)
            val ordersAsMaps = orders.map(order => order.renderedForApi)

            Ok(Serializer.SJSON.toJSON(ordersAsMaps))
          }
        }
      }

      action(accountRequest)
    }
  }
}
