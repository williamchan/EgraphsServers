package services.http.filters

import com.google.inject.Inject

import models.Product
import models.ProductStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Request
import play.api.mvc.Result

/**
 * Filter only where there is a product id that is known.
 */
class RequireProductId @Inject() (productStore: ProductStore) extends Filter[Long, Product] with RequestFilter[Long, Product] {
  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noProductIdResult
  }

  override def filter(productId: Long): Either[Result, Product] = {
    productStore.findById(productId).toRight(left = noProductIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "productId" -> longNumber)
      verifying ("Invalid productId", {
        case productId => productId > 0
      }: Long => Boolean))

  private val noProductIdResult = NotFound("Valid Product ID was required but not provided")
}
