package services.http.filters

import com.google.inject.Inject

import models.PrintOrder
import models.PrintOrderStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Request
import play.api.mvc.Result

/**
 * Filter only where there is an print id that is known.
 */
class RequirePrintOrderId @Inject() (printOrderStore: PrintOrderStore) extends Filter[Long, PrintOrder] with RequestFilter[Long, PrintOrder] {
  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noPrintOrderIdResult
  }

  override def filter(printOrderId: Long): Either[Result, PrintOrder] = {
    printOrderStore.findById(printOrderId).toRight(left = noPrintOrderIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "printOrderId" -> longNumber)
      verifying ("Invalid printOrderId", {
        case printOrderId => printOrderId > 0
      }: Long => Boolean))

  private val noPrintOrderIdResult = NotFound("Valid PrintOrder ID was required but not provided")
}
