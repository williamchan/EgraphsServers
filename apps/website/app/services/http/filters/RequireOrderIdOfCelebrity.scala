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
import models.Order
import play.api.mvc.Request

class RequireOrderIdOfCelebrity @Inject() (orderStore: OrderStore, orderQueryFilters: OrderQueryFilters) {

  /**
   * Only performs `operation` if the request contains an order id of a celebrity that matched
   * an [[models.Order]] in our database.
   * 
   * Otherwise returns a NotFound.
   *
   * @param actionFactory the actionFactory to execute when the order id is found.
   *
   * @return an Action that produces either NotFound or the result of `actionFactory`.
   */
  def apply[A](orderId: Long, celebrityId: Long, parser: BodyParser[A] = parse.anyContent)
  (actionFactory: Order => Action[A])
  : Action[A] = 
  {
    Action(parser) { implicit request =>
      this.asEither(orderId, celebrityId).fold(notFound => notFound, order => actionFactory(order).apply(request))
    }
  }
  
  def asEither[A](orderId: Long, celebrityId: Long)(implicit request: Request[A]): Either[Result, Order] = {
    orderStore.findByCelebrity(celebrityId, orderQueryFilters.orderId(orderId)).headOption match {
      case None => Left(NotFound("The celebrity has no such order"))
      case Some(order) => Right(order)
    }
  }
}
