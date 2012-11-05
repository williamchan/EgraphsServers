package services.http.filters

import com.google.inject.Inject

import models.Order
import models.OrderQueryFilters
import models.OrderStore
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

/**
 * Filters only valid order ids of a celebrity that matched an [[models.Order]] in our database.
 * 
 * Returns a 404 if either the order ID or the celebrity didn't exist.
 */
class RequireOrderIdOfCelebrity @Inject() (orderStore: OrderStore, orderQueryFilters: OrderQueryFilters) extends Filter[(Long, Long), Order] {
  override def filter(orderIdAndCelebrityId: (Long, Long)): Either[Result, Order] = {
    val (orderId, celebrityId) = orderIdAndCelebrityId
    orderStore.findByCelebrity(celebrityId, orderQueryFilters.orderId(orderId)).headOption match {
      case None => Left(NotFound("The celebrity has no such order"))
      case Some(order) => Right(order)
    }
  }
}
