package controllers.api

import models._
import play.mvc.Controller
import sjson.json.Serializer
import models.OrderStore.FindByCelebrity.ActionableOnly
import services.http.CelebrityAccountRequestFilters

private[controllers] trait GetCelebrityOrdersApiEndpoint { this: Controller =>
  protected def orderStore: OrderStore
  protected def actionableOrderFilter: ActionableOnly
  protected def celebFilters: CelebrityAccountRequestFilters

  def getCelebrityOrders(signerActionable: Option[Boolean]) = {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      signerActionable match {
        case None =>
          Error("Please pass in signerActionable=true")

        case Some(false) =>
          Error("signerActionable=false is not a supported filter")

        case _ =>
          val filters = Nil ++ (for (trueValue <- signerActionable) yield actionableOrderFilter)
          val orders = orderStore.FindByCelebrity(celebrity.id, filters: _*)
          val ordersAsMaps = orders.map(order => order.renderedForApi)

          Serializer.SJSON.toJSON(ordersAsMaps)
      }
    }
  }
}
