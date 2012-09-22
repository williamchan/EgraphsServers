package services.http.filters

import com.google.inject.Inject

import models.OrderQueryFilters
import models.OrderStore
import play.api.data.Forms.number
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.NotFound
import play.api.mvc.BodyParser
import play.api.mvc.Action
import play.api.mvc.Result
import services.http.OrderRequest

class RequireOrderIdOfCelebrity @Inject() (orderStore: OrderStore, orderQueryFilters: OrderQueryFilters) {

  /**
   * Only performs `operation` if the request contains an order id of a celebrity that matched
   * an [[models.Order]] in our database.
   * 
   * Otherwise returns a NotFound.
   *
   * @param operation the operation to execute when the order id is found.
   *
   * @return an Action that produces either NotFound or the result of `operation`.
   */
  def apply[A](celebrityId: Long, parser: BodyParser[A] = parse.anyContent)(operation: OrderRequest[A] => Result): Action[A] = {
    Action(parser) { implicit request =>
      Form(single("orderId" -> number)).bindFromRequest.fold(
        errors => NotFound("Order ID was required but not provided"),
        orderId => {
          orderStore.findByCelebrity(celebrityId, orderQueryFilters.orderId(orderId)).headOption match {
            case None =>
              NotFound("The celebrity has no such order")
  
            case Some(order) =>
              operation(OrderRequest(order, request))
          }
        }
      )
    }
  }

}
