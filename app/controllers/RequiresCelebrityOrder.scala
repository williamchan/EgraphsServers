package controllers

import play.mvc.{Before, Controller}
import models.Order
import Order.FindByCelebrity.Filters
import org.squeryl.PrimitiveTypeMode._

/**
 * Provides a high-priority @Before interception that requires the request
 * to have a celebrityId field that is valid and authorized.
 *
 * Mix in to any Controller that already RequiresAuthenticatedAccount
 */
trait RequiresCelebrityOrder { this: Controller with RequiresAuthenticatedAccount with RequiresCelebrity =>
  //
  // Public methods
  //
  protected def order = {
    _order.get
  }

  // Private implementation
  //
  private val _order = new ThreadLocal[Order]

  @Before(priority=30)
  protected def ensureOrderExists = {
    Option(params.get("orderId")) match {
      case None =>
        Error("Order ID was required but not provided")

      case Some(orderIdString) if orderIdString.matches("\\d+") =>
        val orderId = orderIdString.toLong
        inTransaction {
          val orderIdFilter = Order.FindByCelebrity.Filters.OrderId
          Order.FindByCelebrity(celebrity.id, Filters.OrderId(orderId)).headOption match {
            case None =>
              NotFound("The celebrity has no such order")

            case Some(order) =>
              _order.set(order)
              Continue
          }
        }

      case _ =>
        Error("Invalid Order ID")
    }
  }
}