package controllers.website.admin

import models.enums.VideoStatus
import models.Celebrity
import models.VideoAsset
import models.VideoAssetCelebrityStore
import models.VideoAssetStore
import models.website.video.VideoAssetViewModel
import play.api.mvc.Controller
import play.api.mvc.Action
import services.Time.IntsToSeconds.intsToSecondDurations
import services.blobs.Blobs
import services.db.Schema
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.http.WithDBConnection
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetVideoAssetAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def schema: Schema
  protected def videoAssetStore: VideoAssetStore
  protected def videoAssetCelebrityStore: VideoAssetCelebrityStore
  protected def blobs: Blobs

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
                val unsortedVideos = videoAssetStore.getVideosWithStatus(maybeVideoStatus)
                val videos = unsortedVideos.sortBy(_.created.getTime).reverse

                // create a view model for each video, which contains: the secure video URL,
                // the video ID and the associated celebrity's public name
                val videoAssetViewModels = for {
                  video <- videos
                  newVideoUrl <- video.getSecureTemporaryUrl
                  publicName <- videoAssetCelebrityStore.getCelebrityByVideoId(video.id).map(_.publicName)
                } yield {
                  VideoAssetViewModel(
                    videoUrl = newVideoUrl,
                    videoId = video.id,
                    celebrityPublicName = publicName,
                    created=video.created
                  )
                }

                // if above tasks failed for any video, InternalServerError
                if (videoAssetViewModels.length != videos.length)
                  InternalServerError("There was at least one video with improper formatting")
                else {
                  Ok(views.html.Application.admin.admin_videoassets(videoAssetViewModels, status))
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