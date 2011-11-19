package controllers

import libs.Blobs
import play.mvc.Controller
import play.mvc.results.RenderBinary
import Blobs.Conversions._

/**
 * Serves up all Blobs. Particularly useful when served through the file system
 */
object BlobControllers extends Controller with DBTransaction {
  def getBlob(blobKey: String) = {
    Blobs.get(blobKey) match {
      case None =>
        NotFound("No such blob found")

      case Some(data) =>
        new RenderBinary(data.asInputStream, blobKey, true)
    }
  }
}