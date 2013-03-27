package controllers.website.admin

import controllers.PaginationInfoFactory
import models.enums.VideoStatus
import models.Celebrity
import models.VideoAsset
import models.VideoAssetCelebrityStore
import models.VideoAssetStore
import models.website.video.VideoAssetViewModel
import play.api.data._
import play.api.data.Forms._
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

                // Pagination stuff
                val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
                val pagedQuery: (Iterable[VideoAsset], Int, Option[Int]) =
                  services.Utils.pagedQuery(select = unsortedVideos, page = page, pageLength = 5)
                implicit val paginationInfo = PaginationInfoFactory.create(
                  pagedQuery = pagedQuery,
                  pageLength = 5,
                  baseUrl = controllers.routes.WebsiteControllers.getVideoAssetsWithStatusAdmin(status).url
                )

                // Create a view model for each video, which contains: the secure video URL,
                // the video ID and the associated celebrity's public name
                val videoAssetViewModels: List[VideoAssetViewModel] = for {
                  video <- pagedQuery._1.toList
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
                if (videoAssetViewModels.length != pagedQuery._1.size)
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