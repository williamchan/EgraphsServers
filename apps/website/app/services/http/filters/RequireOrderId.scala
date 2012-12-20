package services.http.filters

import com.google.inject.Inject

import models.{Order, OrderStore}
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Request
import play.api.mvc.Result

/**
 * Filter only where there is an order id that is known.
 */
class RequireOrderId @Inject() (orderStore: OrderStore) extends Filter[Long, Order] with RequestFilter[Long, Order] {
  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noOrderIdResult
  }

  override def filter(orderId: Long): Either[Result, Order] = {
    orderStore.findById(orderId).toRight(left = noOrderIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "orderId" -> longNumber)
      verifying ("Invalid orderId", {
      case orderId => orderId > 0
    }: Long => Boolean))

  private val noOrderIdResult = NotFound("Valid Order ID was required but not provided")
}
