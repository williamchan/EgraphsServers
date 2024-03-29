package controllers.website.admin

import models._
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCelebrityInventoryBatchesAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def inventoryBatchStore: InventoryBatchStore
  protected def inventoryBatchQueryFilters: InventoryBatchQueryFilters
  protected def celebrityStore: CelebrityStore

  def getCelebrityInventoryBatchesAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken => 
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>

          // get query parameters
          val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
          val filter: String = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "all", validForm => validForm)

          val query = filter match {
            case "activeOnly" => inventoryBatchStore.findByCelebrity(celebrity.id, inventoryBatchQueryFilters.activeOnly)
            case "all" => inventoryBatchStore.findByCelebrity(celebrity.id)
            case _ => inventoryBatchStore.findByCelebrity(celebrity.id)
          }
          val pagedQuery: (Iterable[InventoryBatch], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
          implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebrityInventoryBatchesAdminEndpoint.url(celebrity.id), filter = Option(filter))

          // also display information from catalog stars query for manual discrepancy resolution
          val stars = celebrityStore.getCatalogStars.filter(star => star.id == celebrity.id)
          val inventoryRemaining = if(stars.size == 1) {
            stars.head.inventoryRemaining
          } else {
            0
          }

          Ok(views.html.Application.admin.admin_inventorybatches(inventoryBatches = pagedQuery._1, celebrity = celebrity, inventoryRemaining = inventoryRemaining))
        }
      }
    }
  }
}

object GetCelebrityInventoryBatchesAdminEndpoint {

  def url(celebrityId: Long) = {
    controllers.routes.WebsiteControllers.getCelebrityInventoryBatchesAdmin(celebrityId = celebrityId).url
  }
}
