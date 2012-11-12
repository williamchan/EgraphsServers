package controllers.website.admin
import play.api.mvc.{ Action, Controller }
import models.enums.VideoStatus
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.db.Schema
import services.http.WithDBConnection
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._

trait PostProcessVideoAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def schema: Schema

  def postProcessVideo(action: String, url: String) = controllerMethod(WithDBConnection(readOnly = false)) {
    Action { request =>
      
      val videoStatusUpdated = update(schema.videoAssets)(
          s => where(s.url === url) set(s._videoStatus := action))

      Redirect(controllers.routes.WebsiteControllers.getUnprocessedVideos)
    }
  }
}