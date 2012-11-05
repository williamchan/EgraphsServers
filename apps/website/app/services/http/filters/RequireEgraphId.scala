package services.http.filters

import com.google.inject.Inject

import models.Egraph
import models.EgraphStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Request
import play.api.mvc.Result

/**
 * Filter only where there is an egraph id that is known.
 */
class RequireEgraphId @Inject() (egraphStore: EgraphStore) extends Filter[Long, Egraph] with RequestFilter[Long, Egraph] {
  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noEgraphIdResult
  }

  override def filter(egraphId: Long): Either[Result, Egraph] = {
    egraphStore.findById(egraphId).toRight(left = noEgraphIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "egraphId" -> longNumber)
      verifying ("Invalid egraphId", {
        case egraphId => egraphId > 0
      }: Long => Boolean))

  private val noEgraphIdResult = NotFound("Valid Egraph ID was required but not provided")
}
