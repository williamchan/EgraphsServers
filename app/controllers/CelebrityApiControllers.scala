package controllers

import play.mvc.Controller
import sjson.json.Serializer
import models.Order

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

  def getOrders(fulfilled: Boolean = true) = {
    import org.squeryl.PrimitiveTypeMode._
    
    inTransaction {
      val orders = Order.findByCelebrity(celebrity.id, !fulfilled)
      val ordersAsApiMaps = orders.map(order => order.renderedForApi)

      Serializer.SJSON.toJSON(ordersAsApiMaps)
    }
  }
}