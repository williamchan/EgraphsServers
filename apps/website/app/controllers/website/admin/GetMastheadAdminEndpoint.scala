package controllers.website.admin

import services.mvc.ImplicitHeaderAndFooterData
import play.api.mvc.{Request, Action, Controller}
import services.http.ControllerMethod
import models.{Masthead, MastheadStore}
import services.http.filters.HttpFilters


private[controllers] trait GetMastheadAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def mastheadStore: MastheadStore

  private val postMastheadUrl = controllers.routes.WebsiteControllers.postMastheadAdmin.url
  def getMastheadAdmin(mastheadId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action {implicit request =>
        val maybeMasthead = mastheadStore.findById(mastheadId)
        maybeMasthead match {
          case Some(masthead) => Ok(views.html.Application.admin.admin_masthead_detail(masthead, postMastheadUrl, errorFields))
          case None => NotFound("No masthead with this ID exists")
        }

      }
    }
  }

  def getCreateMastheadAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action {implicit request =>
        Ok(views.html.Application.admin.admin_masthead_detail(Masthead(
          headline = "An Example headline",
          subtitle = Some("an example subtitle")
        ), postMastheadUrl, errorFields))
      }
    }
  }

  private def errorFields(implicit request: Request[play.api.mvc.AnyContent]) : Option[List[String]] = flash.get("errors").map(errString => errString.split(',').toList)

}
