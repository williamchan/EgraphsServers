package controllers.website

import services.blobs.Blobs
import play.api._
import play.api.mvc._
import Blobs.Conversions._
import services.http.ControllerMethod
import play.api.libs.MimeTypes
import play.api.http.ContentTypeOf
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
  def getBlob(blobKey: String) = Action { implicit request =>
    controllerMethod(openDatabase=false) {
      blobs.get(blobKey) match {
        case None =>
          NotFound("No such blob found")
  
        case Some(data) => {
          // Play's Mime-Type logic doesn't correctly map .svgz to the correct Content-Encoding
          // and Content-Type.
          val (contentType, header) = if (blobKey.endsWith("svgz")) {
            (new ContentTypeOf(MimeTypes.forExtension(".svg")), ("Content-Encoding", "gzip"))
          } else {
            (new ContentTypeOf(MimeTypes.forFileName(blobKey)), ("",""))
          }
  
          log("Serving blob \"" + blobKey + "\" with content type \"" + contentType + "\"")
          //TODO: need file here, not input stream
          Ok.sendFile(data.asInputStream, blobKey, true).withHeaders(header).as(contentType.toString())
        }
      }
    }
  }
}

object GetBlobEndpoint extends Logging