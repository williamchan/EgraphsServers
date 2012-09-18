package controllers.website.admin

import play.api.mvc.Controller
import models._
import services.http.{ControllerMethod, AdminRequestFilters}
import play.mvc.Router.ActionDefinition
import controllers.WebsiteControllers
import services.Utils

private[controllers] trait GetCelebrityInventoryBatchesAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def inventoryBatchStore: InventoryBatchStore
  protected def inventoryBatchQueryFilters: InventoryBatchQueryFilters

  def getCelebrityInventoryBatchesAdmin(filter: String = "all", page: Int = 1) = controllerMethod() {
    adminFilters.requireCelebrity {
      (celebrity, admin) =>
        val query = filter match {
          case "activeOnly" => inventoryBatchStore.findByCelebrity(celebrity.id, inventoryBatchQueryFilters.activeOnly)
          case "all" => inventoryBatchStore.findByCelebrity(celebrity.id)
          case _ => inventoryBatchStore.findByCelebrity(celebrity.id)
        }
        val pagedQuery: (Iterable[InventoryBatch], Int, Option[Int]) = Utils.pagedQuery(select = query, page = page)
        WebsiteControllers.updateFlashScopeWithPagingData(pagedQuery = pagedQuery, baseUrl = GetCelebrityInventoryBatchesAdminEndpoint.url(celebrity), filter = Some(filter))
        views.html.Application.admin.admin_inventorybatches(inventoryBatches = pagedQuery._1, celebrity = celebrity)
    }
  }
}

object GetCelebrityInventoryBatchesAdminEndpoint {

  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl("WebsiteControllers.getCelebrityInventoryBatchesAdmin", Map("celebrityId" -> celebrity.id.toString))
  }
}
