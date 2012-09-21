package services.http

import play.api.mvc.Request
import com.google.inject.Inject
import play.mvc.results.NotFound
import models.{OrderQueryFilters, OrderStore, Order}

class OrderRequestFilters @Inject() (orderStore: OrderStore, orderQueryFilters: OrderQueryFilters) {
  import SafePlayParams.Conversions._

  def requireOrderIdOfCelebrity(celebrityId: Long)(onAllow: Order => Any)(implicit request: Request) = {
    request.params.getOption("orderId") match {
      case None =>
        new Error("Order ID was required but not provided")

      case Some(orderIdString) if orderIdString.matches("\\d+") =>
        val orderId = orderIdString.toLong
        orderStore.findByCelebrity(celebrityId, orderQueryFilters.orderId(orderId)).headOption match {
          case None =>
            new NotFound("The celebrity has no such order")

          case Some(order) =>
            onAllow(order)
        }

      case _ =>
        new Error("Invalid Order ID")
    }
  }
}
