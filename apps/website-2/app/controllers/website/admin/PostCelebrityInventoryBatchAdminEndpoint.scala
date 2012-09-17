package controllers.website.admin

import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.api.mvc.Controller
import models._
import services.logging.Logging
import java.util.Date
import play.data.validation.Validation
import services.http.{POSTControllerMethod, CelebrityAccountRequestFilters, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._

trait PostCelebrityInventoryBatchAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def inventoryBatchStore: InventoryBatchStore

  def postCelebrityInventoryBatchAdmin(inventoryBatchId: Long = 0,
                                       numInventory: Int,
                                       startDate: Date,
                                       endDate: Date) = postController() {
    celebFilters.requireCelebrityId(request) { celebrity =>
      val isCreate = (inventoryBatchId == 0)

      Validation.min("Inventory Amount must be non-negative", numInventory, 0)
      Validation.required("Start Date", startDate)
      Validation.required("End Date", endDate)

      if (validationErrors.isEmpty) {
        val products = celebrity.products().toList
        val productsSelected = products.filter(p => params.getOption("prod" + p.id).isDefined)
        if (isCreate) {
          val inventoryBatch = InventoryBatch(celebrityId = celebrity.id, numInventory = numInventory, startDate = startDate, endDate = endDate).save()
          for (p <- productsSelected) {
            inventoryBatch.products.associate(p)
          }
        } else {
          val ib = inventoryBatchStore.get(inventoryBatchId)
          val inventoryBatch = ib.copy(numInventory = numInventory, startDate = startDate, endDate = endDate).save()

          // todo(wchan): This logic is generally useful for ManyToManys
          val productsAlreadyInInventoryBatch = inventoryBatch.products.toList
          val productsToAssociate = productsSelected.diff(productsAlreadyInInventoryBatch)
          val productsToDissociate = productsAlreadyInInventoryBatch.diff(productsSelected)
          for (productToAssociate <- productsToAssociate) {
            inventoryBatch.products.associate(productToAssociate)
          }
          for (productToDissociate <- productsToDissociate) {
            inventoryBatch.products.dissociate(productToDissociate)
          }
        }

        new Redirect(GetCelebrityInventoryBatchesAdminEndpoint.url(celebrity = celebrity).url)
      }
      else {
        // There were validation errors
        redirectWithValidationErrors(celebrity, inventoryBatchId, numInventory, startDate, endDate)
      }
    }
  }

  private def redirectWithValidationErrors(celebrity: Celebrity,
                                           inventoryBatchId: Long,
                                           numInventory: Int,
                                           startDate: Date,
                                           endDate: Date): Redirect = {
    flash.put("inventoryBatchId", inventoryBatchId)
    flash.put("numInventory", numInventory)
    flash.put("startDate", startDate)
    flash.put("endDate", endDate)
    if (inventoryBatchId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityInventoryBatchAdminEndpoint.url(celebrity = celebrity))
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetInventoryBatchAdminEndpoint.url(inventoryBatchId = inventoryBatchId))
    }
  }
}
