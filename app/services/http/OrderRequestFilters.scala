package services.http

import models.OrderStore.FindByCelebrity
import models.{OrderStore, Order}
import play.mvc.Http.Request
import com.google.inject.Inject
import play.mvc.results.NotFound

class OrderRequestFilters @Inject() (orderStore: OrderStore) {
  import OptionParams.Conversions._

  def requireOrderIdOfCelebrity(celebrityId: Long)(onAllow: Order => Any)(implicit request: Request) = {
    request.params.getOption("orderId") match {
      case None =>
        new Error("Order ID was required but not provided")

      case Some(orderIdString) if orderIdString.matches("\\d+") =>
        val orderId = orderIdString.toLong
        orderStore.FindByCelebrity(celebrityId, FindByCelebrity.OrderId(orderId)).headOption match {
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