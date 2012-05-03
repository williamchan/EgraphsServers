package controllers.website.admin

import play.mvc.Controller
import services.Utils
import services.http.{ControllerMethod, AdminRequestFilters}
import models.InventoryBatchStore

private[controllers] trait GetInventoryBatchAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def inventoryBatchStore: InventoryBatchStore

  def getInventoryBatchAdmin(inventoryBatchId: Long, action: Option[String] = None) = controllerMethod() {
    adminFilters.requireAdministratorLogin { admin =>
      val inventoryBatchOption = inventoryBatchStore.findById(inventoryBatchId)
      val inventoryBatch = inventoryBatchOption.get

      flash.put("inventoryBatchId", inventoryBatch.id)
      flash.put("numInventory", inventoryBatch.numInventory)
      flash.put("startDate", inventoryBatch.startDate)
      flash.put("endDate", inventoryBatch.endDate)

      GetInventoryBatchDetail.getCelebrityInventoryBatchDetail(celebrity = inventoryBatch.celebrity, inventoryBatch = inventoryBatchOption)
    }
  }
}

object GetInventoryBatchAdminEndpoint {

  def url(inventoryBatchId: Long) = {
    Utils.lookupUrl("WebsiteControllers.getInventoryBatchAdmin", Map("inventoryBatchId" -> inventoryBatchId.toString))
  }
}