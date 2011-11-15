package controllers

import play.mvc.Controller
import sjson.json.Serializer
import models.{ActionableFilter, Order}

/**
 * Controllers that handle direct API requests for celebrity resources.
 */
object CelebrityApiControllers extends Controller
  with RequiresAuthenticatedAccount
  with RequiresCelebrity
{

  def getCelebrity = {
    Serializer.SJSON.toJSON(celebrity.renderedForApi)
  }

  def getOrders(signerActionable: Option[Boolean]) = {
    import org.squeryl.PrimitiveTypeMode._

    signerActionable match {
      case Some(false) =>
        Error("signerActionable=false is not a supported filter")

      case _ =>
        val filters = Nil ++ (for (trueValue <- signerActionable) yield ActionableFilter)

        inTransaction {
          val orders = Order.findByCelebrity(celebrity.id, filters: _*)
          val ordersAsApiMaps = orders.map(order => order.renderedForApi)

          Serializer.SJSON.toJSON(ordersAsApiMaps)
        }
    }
  }
}