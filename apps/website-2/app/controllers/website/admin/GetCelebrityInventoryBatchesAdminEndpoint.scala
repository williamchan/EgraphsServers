package controllers.website.admin

import play.api.mvc.Controller
import models._
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory

private[controllers] trait GetCelebrityInventoryBatchesAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def inventoryBatchStore: InventoryBatchStore
  
  import services.AppConfig.instance
  private def inventoryBatchQueryFilters = instance[InventoryBatchQueryFilters]

  def getCelebrityInventoryBatchesAdmin(celebrityId: Long, filter: String = "all", page: Int = 1) = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, account) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>
          val query = filter match {
            case "activeOnly" => inventoryBatchStore.findByCelebrity(celebrity.id, inventoryBatchQueryFilters.activeOnly)
            case "all" => inventoryBatchStore.findByCelebrity(celebrity.id)
            case _ => inventoryBatchStore.findByCelebrity(celebrity.id)
          }
          val pagedQuery: (Iterable[InventoryBatch], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
          implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
          Ok(views.html.Application.admin.admin_inventorybatches(inventoryBatches = pagedQuery._1, celebrity = celebrity))
        }
      }
    }
  }
}

object GetCelebrityInventoryBatchesAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCelebrityInventoryBatchesAdmin(celebrityId = celebrity.id).url
  }
}
