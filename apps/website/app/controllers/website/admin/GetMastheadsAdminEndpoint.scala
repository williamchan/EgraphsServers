package controllers.website.admin

import services.mvc.ImplicitHeaderAndFooterData
import play.api.mvc.{Action, Controller}
import services.http.ControllerMethod
import models.MastheadStore
import services.http.filters.HttpFilters
import models.categories.{Featured, MastheadCategoryValueStore}


private[controllers] trait GetMastheadsAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def mastheadStore: MastheadStore
  protected def mastheadCategoryValueStore: MastheadCategoryValueStore
  protected def featured: Featured

  def getMastheadsAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action {implicit request =>
        val mastheads = mastheadStore.getAll
        val featuredMastheadIds = mastheadCategoryValueStore.findByCategoryValueId(featured.categoryValue.id).map(mcv => mcv.mastheadId)
        Ok(views.html.Application.admin.admin_mastheads(mastheads, featuredMastheadIds))
      }
    }
  }
}
