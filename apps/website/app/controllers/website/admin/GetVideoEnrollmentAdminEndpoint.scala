package controllers.website.admin

import services.mvc.ImplicitHeaderAndFooterData
import play.api._
import play.api.mvc._
import services.http.filters.HttpFilters
import services.AppConfig
import services.http.{ ControllerMethod, WithDBConnection }
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import models.VideoAsset
import services.db.Schema

private[controllers] trait GetVideoEnrollmentAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def schema: Schema

  def getVideoEnrollmentAdmin =
    controllerMethod.withForm() { implicit authToken =>
      httpFilters.requireAdministratorLogin.inSession() {
        case (admin, adminAccount) =>
          Action { implicit request =>
            Ok(views.html.Application.admin.admin_videoasset())
          }
      }
    }

  def getUnprocessedVideosAdmin = controllerMethod(WithDBConnection(readOnly = true)) {
    controllerMethod.withForm() { implicit authToken =>
      httpFilters.requireAdministratorLogin.inSession() {
        case (admin, adminAccount) =>
          Action { implicit request =>

            val videosAwaitingProcessing: Query[(String)] = from(schema.videoAssets)(
              s => where(s._videoStatus === "Unprocessed") select (s.url))

            val list = videosAwaitingProcessing.toList
            Ok(views.html.Application.admin.admin_unprocessedvideos(list))
          }
      }
    }
  }
}