package controllers.website.admin

import play.api.mvc.{ Action, Controller }
import models.enums.VideoStatus
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.db.Schema
import services.http.WithDBConnection
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import models.VideoAssetStore
import services.http.POSTControllerMethod

trait PostProcessVideoAdminEndpoint {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def videoAssetStore: VideoAssetStore
  protected def schema: Schema

  def postProcessVideo(action: String, id: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>

          val maybeVideoStatus = VideoStatus(action)
          maybeVideoStatus match {
            case None => BadRequest("Unknown action")
            case Some(maybeVideoStatus) => {

              val maybeVideoAsset = videoAssetStore.findById(id)
              maybeVideoAsset match {
                case None => BadRequest("The video asset with given ID was not found.")
                case Some(videoAsset) => {
                  videoAsset.withVideoStatus(maybeVideoStatus).save()
                  Redirect(controllers.routes.WebsiteControllers.getUnprocessedVideosAdmin)
                }
              }
            }
          }
        }
    }
  }
}