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

private[controllers] trait GetVideoEnrollmentEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected val controllerMethod = AppConfig.instance[ControllerMethod]
  protected val filters = AppConfig.instance[HttpFilters]
  protected def schema: Schema

  def getVideoEnrollment = Action {
    Ok(views.html.Application.admin.admin_videoasset())
  }

  def getUnprocessedVideos = filters.requireApplicationId.test {
    controllerMethod(dbSettings = WithDBConnection(readOnly = true)) {
      Action {

        // get all unprocessed videos
        //        val videosAwaitingProcessing: Query[(VideoAsset)] = from(schema.videoAssets)(
        //          s => where(s._videoStatus === "Unprocessed") select (s))

        val videosAwaitingProcessing: Query[(String)] = from(schema.videoAssets)(
          s => where(s._videoStatus === "Unprocessed") select (s.url))

        val list = videosAwaitingProcessing.toList

        for (video <- list) {
          play.Logger.info("Video to process: " + video)
        }

        Ok(views.html.Application.admin.admin_unprocessedvideos(list))

      }
    }
  }
}