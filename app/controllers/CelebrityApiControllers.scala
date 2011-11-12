package controllers

import play.mvc.Controller
import sjson.json.Serializer
import models.{FindByCelebrityFilter, UnfulfilledFilter, Order}

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

  def getOrders(fulfilled: Boolean = false) = {
    import org.squeryl.PrimitiveTypeMode._

    val filters = List.empty[FindByCelebrityFilter] ++
      (if (fulfilled) List.empty else List(UnfulfilledFilter))

    inTransaction {
      val orders = Order.findByCelebrity(celebrity.id, filters: _*)
      val ordersAsApiMaps = orders.map(order => order.renderedForApi)

      Serializer.SJSON.toJSON(ordersAsApiMaps)
    }
  }
}