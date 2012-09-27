package controllers.website

import services.blobs.Blobs
import play.mvc.Controller
import play.mvc.results.RenderBinary
import Blobs.Conversions._
import services.http.{WithoutDBConnection, ControllerMethod}
import play.libs.MimeTypes
import play.mvc.Http.Response
import services.logging.Logging

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
private[controllers] trait GetBlobEndpoint { this: Controller =>
  import GetBlobEndpoint._

  protected def blobs: Blobs
  protected def controllerMethod: ControllerMethod

  // TODO: Cache these results. This endpoint will become extremely expensive
  // if we launch this way.
  def getBlob(blobKey: String) = controllerMethod(dbSettings = WithoutDBConnection) {

    blobs.get(blobKey) match {
      case None =>
        NotFound("No such blob found")

      case Some(data) =>
        // Play's Mime-Type logic doesn't correctly map .svgz to the correct Content-Encoding
        // and Content-Type.
        val mimeType = if (blobKey.endsWith("svgz")) {
          Response.current().setHeader("Content-Encoding", "gzip")
          MimeTypes.getContentType(".svg")
        }
        else {
          MimeTypes.getContentType(blobKey)
        }

        log("Serving blob \"" + blobKey + "\" with mime type \"" + mimeType + "\"")
        Response.current().setContentTypeIfNotSet(mimeType)
        new RenderBinary(data.asInputStream, blobKey, true)
    }
  }
}

object GetBlobEndpoint extends Logging