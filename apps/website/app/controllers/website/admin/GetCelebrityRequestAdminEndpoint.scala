package controllers.website.admin

import models._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetCelebrityRequestAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def celebrityRequestStore: CelebrityRequestStore  
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  def getCelebrityRequestsAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>
          val celebrityRequests = celebrityRequestStore.getAll
          Ok(views.html.Application.admin.admin_celebrityrequests(celebrityRequests))
        }
    }
  }  
}