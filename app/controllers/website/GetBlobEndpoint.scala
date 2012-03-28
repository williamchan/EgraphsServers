package controllers.website

import services.blobs.Blobs
import play.mvc.Controller
import play.mvc.results.RenderBinary
import Blobs.Conversions._
import services.http.ControllerMethod

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
private[controllers] trait GetBlobEndpoint { this: Controller =>
  protected def blobs: Blobs
  protected def controllerMethod: ControllerMethod

  // TODO: Cache these results. This endpoint will become extremely expensive
  // if we launch this way.
  def getBlob(blobKey: String) = controllerMethod() {
    blobs.get(blobKey) match {
      case None =>
        NotFound("No such blob found")

      case Some(data) =>
        new RenderBinary(data.asInputStream, blobKey, true)
    }
  }
}

