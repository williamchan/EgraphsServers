package controllers.website.admin

import play.api.mvc.Controller
import models.InventoryBatchStore
import controllers.WebsiteControllers
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect, NotFound}
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetInventoryBatchAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def inventoryBatchStore: InventoryBatchStore

  def getInventoryBatchAdmin(inventoryBatchId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireInventoryBatchId(inventoryBatchId) { inventoryBatch =>
        Action { implicit request =>
          implicit val flash = request.flash + 
          	("inventoryBatchId" -> inventoryBatch.id.toString) + 
          	("numInventory" -> inventoryBatch.numInventory.toString) + 
          	("startDate" -> inventoryBatch.startDate.toString) + 
          	("endDate" -> inventoryBatch.endDate.toString)
          GetInventoryBatchDetail.getCelebrityInventoryBatchDetail(celebrity = inventoryBatch.celebrity, inventoryBatch = Option(inventoryBatch))
        }
      }
    }
  }
}

object GetInventoryBatchAdminEndpoint {

  def url(inventoryBatchId: Long) = {
    controllers.routes.WebsiteControllers.getInventoryBatchAdmin(inventoryBatchId).url
  }
}