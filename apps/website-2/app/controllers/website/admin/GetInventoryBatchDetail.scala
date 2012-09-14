package controllers.website.admin

import models.Celebrity
import play.mvc.Scope.{Session, Flash}
import play.templates.Html

object GetInventoryBatchDetail {

  def getCelebrityInventoryBatchDetail(celebrity: Celebrity,
                                       inventoryBatch: Option[models.InventoryBatch] = None)(implicit flash: Flash, session: Session): Html = {
    val isCreate = inventoryBatch.isEmpty

    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "inventoryBatchId" => flash.get("inventoryBatchId")
        case "numInventory" => flash.get("numInventory")
        case "startDate" => flash.get("startDate")
        case "endDate" => flash.get("endDate")
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }

    val products = if (isCreate) {
      celebrity.products().map(p => (p, false))
    } else {
      val productsAlreadyInInventoryBatch = inventoryBatch.get.products.toList
      val productsNotInInventoryBatch = celebrity.products().toList.diff(productsAlreadyInInventoryBatch)
      val ps = productsAlreadyInInventoryBatch.map(p => (p, true)) ::: productsNotInInventoryBatch.map(p => (p, false))
      ps.sortWith((tuple1, tuple2) => tuple1._1.id < tuple2._1.id)
    }

    // Render the page
    views.Application.admin.html.admin_inventorybatchdetail(
      isCreate = isCreate,
      celebrity = celebrity,
      errorFields = errorFields,
      fields = fieldDefaults,
      inventoryBatch = inventoryBatch,
      products = products,
      remainingInventory = inventoryBatch.map(ib => ib.getRemainingInventory)
    )
  }

}
