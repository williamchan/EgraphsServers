package services.http.filters

import scala.annotation.implicitNotFound

import com.google.inject.Inject

import models.InventoryBatch
import models.InventoryBatchStore
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Request
import play.api.mvc.Result

/**
 * Filter only where there is an inventory batch id that is known.
 */
class RequireInventoryBatchId @Inject() (inventoryBatchStore: InventoryBatchStore) extends Filter[Long, InventoryBatch] with RequestFilter[Long, InventoryBatch] {
  override protected def formFailedResult[A, S >: Source](formWithErrors: Form[Long], source: S)(implicit request: Request[A]): Result = {
    noInventoryBatchIdResult
  }

  override def filter(inventoryBatchId: Long): Either[Result, InventoryBatch] = {
    inventoryBatchStore.findById(inventoryBatchId).toRight(left = noInventoryBatchIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "inventoryBatchId" -> longNumber)
      verifying ("Invalid inventoryBatchId", {
        case inventoryBatchId => inventoryBatchId > 0
      }: Long => Boolean))

  private val noInventoryBatchIdResult = NotFound("Valid InventoryBatch ID was required but not provided")
}
