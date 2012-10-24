package controllers.website.admin

import models.Celebrity
import play.api.mvc.Results.Ok

object GetInventoryBatchDetail {

  def getCelebrityInventoryBatchDetail(celebrity: Celebrity, inventoryBatch: Option[models.InventoryBatch] = None
      )(implicit authToken: egraphs.authtoken.AuthenticityToken, 
                 headerData: models.frontend.header.HeaderData, 
                 footerData: models.frontend.footer.FooterData,   
                 flash: play.api.mvc.Flash): play.api.mvc.Result = {
    
    val isCreate = inventoryBatch.isEmpty

    val errorFields = flash.get("errors").map(errString => errString.split(',').toList)

    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "inventoryBatchId" => flash.get("inventoryBatchId").getOrElse("")
        case "numInventory" => flash.get("numInventory").getOrElse("")
        case "startDate" => flash.get("startDate").getOrElse("")
        case "endDate" => flash.get("endDate").getOrElse("")
        case _ => flash.get(paramName).getOrElse("")
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

    Ok(views.html.Application.admin.admin_inventorybatchdetail(
      isCreate = isCreate,
      celebrity = celebrity,
      errorFields = errorFields,
      fields = fieldDefaults,
      inventoryBatch = inventoryBatch,
      products = products,
      remainingInventory = inventoryBatch.map(ib => ib.getRemainingInventory)
    ))
  }

}
