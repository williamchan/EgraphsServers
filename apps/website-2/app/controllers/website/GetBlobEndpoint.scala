package controllers.website

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import services.blobs.Blobs.Conversions._
import services.blobs.Blobs
import services.http.ControllerMethod
import services.logging.Logging
import play.api.http.ContentTypeOf
import play.api.libs.MimeTypes
import play.api.libs.iteratee.Enumerator

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
private[controllers] trait GetBlobEndpoint { this: Controller =>
  import GetBlobEndpoint._

  protected def blobs: Blobs
  protected def controllerMethod: ControllerMethod

  // TODO: Cache these results. This endpoint will become extremely expensive
  // if we launch this way.
  def getBlob(blobKey: String) = {
    // This line protects us from an obscure compiler bug.
    // Touch it if you want to have a bad time. (Scala 2.9.1)
    val thisControllerMethod = controllerMethod
    
    thisControllerMethod(openDatabase=false) {
      Action {
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
          
          implicit val responseContentType = contentType
          val dataContent = Enumerator.fromStream(data.asInputStream)
          
          Ok.stream(dataContent).withHeaders(header)
        }
      }
      }
    } 
  }
}

object GetBlobEndpoint extends Logging