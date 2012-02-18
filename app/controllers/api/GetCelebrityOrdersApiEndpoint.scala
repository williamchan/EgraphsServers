package controllers.api

import models._
import play.mvc.Controller
import sjson.json.Serializer
import services.http.CelebrityAccountRequestFilters

private[controllers] trait GetCelebrityOrdersApiEndpoint { this: Controller =>
  protected def orderStore: OrderStore
  protected def orderQueryFilters: OrderQueryFilters
  protected def celebFilters: CelebrityAccountRequestFilters

  /**
   * Provides a JSON array of a celebrity's Orders for consumption
   * by the API. See the
   * [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints#APIEndpoints-Orders%C2%A0Orders JSON specification]]
   *
   * @param signerActionable true if the only Orders that should
   *   be returned are the ones that the celebrity needs to sign.
   */
  def getCelebrityOrders(signerActionable: Option[Boolean]) = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      signerActionable match {
        case None =>
          Error("Please pass in signerActionable=true")

        case Some(false) =>
          Error("signerActionable=false is not a supported filter")

        case _ =>
          val filters = Nil ++ (for (trueValue <- signerActionable) yield orderQueryFilters.actionableOnly)
          val orders = orderStore.findByCelebrity(celebrity.id, filters: _*)
          val ordersAsMaps = orders.map(order => order.renderedForApi)

          Serializer.SJSON.toJSON(ordersAsMaps)
      }
    }
  }
}
