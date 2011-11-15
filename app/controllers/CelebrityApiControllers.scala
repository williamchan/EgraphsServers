package controllers

import play.mvc.Controller
import sjson.json.Serializer
import models.Order
import models.Order.FindByCelebrity.Filters

/**
 * Controllers that handle direct API requests for celebrity resources.
 */
object CelebrityApiControllers extends Controller
  with RequiresAuthenticatedAccount
  with RequiresCelebrity
  with DBTransaction
{

  def getCelebrity = {
    Serializer.SJSON.toJSON(celebrity.renderedForApi)
  }

  def getOrders(signerActionable: Option[Boolean]) = {
    signerActionable match {
      case Some(false) =>
        Error("signerActionable=false is not a supported filter")

      case _ =>
        val filters = Nil ++ (for (trueValue <- signerActionable) yield Filters.ActionableOnly)
        val orders = Order.FindByCelebrity(celebrity.id, filters: _*)
        val ordersAsMaps = orders.map(order => order.renderedForApi)

        Serializer.SJSON.toJSON(ordersAsMaps)
    }
  }
}