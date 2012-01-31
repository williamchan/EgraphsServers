package controllers.browser

import services.Blobs
import play.mvc.Controller
import play.mvc.results.RenderBinary
import Blobs.Conversions._
import services.AppConfig

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
private[controllers] trait GetBlobEndpoint { this: Controller =>
  protected def blobs: Blobs

  def getBlob(blobKey: String) = {
    Blobs.get(blobKey) match {
      case None =>
        NotFound("No such blob found")

      case Some(data) =>
        new RenderBinary(data.asInputStream, blobKey, true)
    }
  }
}

