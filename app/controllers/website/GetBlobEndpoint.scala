package controllers.website

import services.blobs.Blobs
import play.mvc.Controller
import play.mvc.results.RenderBinary
import Blobs.Conversions._

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
private[controllers] trait GetBlobEndpoint { this: Controller =>
  protected def blobs: Blobs

  // TODO: Cache these results. This endpoint will become extremely expensive
  // if we launch this way.
  def getBlob(blobKey: String) = {
    blobs.get(blobKey) match {
      case None =>
        NotFound("No such blob found")

      case Some(data) =>
        new RenderBinary(data.asInputStream, blobKey, true)
    }
  }
}

