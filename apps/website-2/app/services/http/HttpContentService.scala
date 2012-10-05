package services.http

import play.api.libs.MimeTypes
import play.api.http.ContentTypeOf

/**
 * Gives access to HTTP content header information for different file names.
 */
class HttpContentService {
  def headersForFilename(fileName: String): ContentHeaders = {
    fileExtensionRegex.findFirstMatchIn(fileName) match {
      case Some(strMatch) if strMatch.group(1) == "svgz"  =>
        SVGZContentHeaders

      case _ =>
        val mimeType = MimeTypes.forFileName(fileName)
        new ContentHeaders(mimeType.getOrElse("application/octet-stream"), None)
    }
  }

  //
  // Private Methods
  //
  private def fileExtensionRegex = "^.*\\.([^.]+)$".r
}

/**
 * Encapsulates the Content-* HTTP headers for particular content.
 *
 * @param contentType Value for the "Content-Type" HTTP header for this content
 * @param contentEncoding Value for the "Content-Encoding" HTTP header for this content
 */
class ContentHeaders(val contentType: String, val contentEncoding: Option[String])


/** ContentInfo specific for SVGZ files, which Play doesn't naturally handle well. */
private[http] case object SVGZContentHeaders
  extends ContentHeaders("image/svg+xml", Some("gzip"))
