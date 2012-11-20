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
import models.enums.VideoStatus
import models.VideoAssetStore
import models.VideoAssetCelebrityStore
import models.CelebrityStore
import models.Celebrity

private[controllers] trait GetVideoAssetAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def schema: Schema
  protected def videoAssetStore: VideoAssetStore
  protected def videoAssetCelebrityStore: VideoAssetCelebrityStore

  def getVideoAssetAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>
          Ok(views.html.Application.admin.admin_videoasset())
        }
    }
  }

  def getVideoAssetsWithStatusAdmin(status: String) = controllerMethod(WithDBConnection(readOnly = true)) {
    controllerMethod.withForm() { implicit authToken =>
      httpFilters.requireAdministratorLogin.inSession() {
        case (admin, adminAccount) =>
          Action { implicit request =>

            val maybeVideoStatus = VideoStatus(status)
            maybeVideoStatus match {
              case None => BadRequest("Unknown status: " + status)
              case Some(maybeVideoStatus) => {
                val videos = videoAssetStore.getVideosWithStatus(maybeVideoStatus)

                val maybeVideosAndPublicNames: List[(VideoAsset, Option[String])] = for (video <- videos) yield {
                  val maybeCelebrity: Option[Celebrity] = videoAssetCelebrityStore.getCelebrityByVideoId(video.id)
                  val maybePublicName = maybeCelebrity.map(_.publicName)
                  (video, maybePublicName)
                }

                if (maybeVideosAndPublicNames.exists { case (_, maybePublicName) => maybePublicName.isEmpty })
                  InternalServerError("There was at least one video with no associated celebrity public name")
                else {
                  val videosAndPublicNames = maybeVideosAndPublicNames.map { case (video, maybePublicName) => (video, maybePublicName.get) }
                  Ok(views.html.Application.admin.admin_videoassets(videosAndPublicNames, status))
                }
              }
            }
          }
      }
    }
  }

  def getVideoAssetsAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>
          Redirect(controllers.routes.WebsiteControllers.getVideoAssetsWithStatusAdmin(VideoStatus.Unprocessed.name))
        }
    }
  }
}