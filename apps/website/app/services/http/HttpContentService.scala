package services.http

import play.libs.MimeTypes

/**
 * Gives access to HTTP content header information for different file names.
 */
class HttpContentService {
  def headersForFilename(fileName: String): ContentHeaders = {
    fileExtensionRegex.findFirstMatchIn(fileName) match {
      case Some(strMatch) if strMatch.group(1) == "svgz"  =>
        SVGZContentHeaders

      case _ =>
        val mimeType = playContentType(fileName)
        val contentEncoding = Option(play.utils.HTTP.parseContentType(mimeType).encoding)
        ContentHeaders(mimeType, contentEncoding)
    }
  }

  //
  // Private Methods
  //
  private def fileExtensionRegex = "^.*\\.([^.]+)$".r

  private def playContentType(fileName: String) = {
    try {
      MimeTypes.getContentType(fileName)
    }
    catch {
      // TODO: remove this ridiculous NPE check once Play fixes this problem:
      // https://play.lighthouseapp.com/projects/57987-play-framework/tickets/1519-mimetypesgetcontenttype-nullpointerexception#ticket-1519-2
      case npe: NullPointerException => MimeTypes.getMimeType(fileName)
    }
  }
}


/**
 * Encapsulates the Content-* HTTP headers for particular content.
 *
 * @param contentType Value for the "Content-Type" HTTP header for this content
 * @param contentEncoding Value for the "Content-Encoding" HTTP header for this content
 */
case class ContentHeaders(contentType: String, contentEncoding: Option[String])


/** ContentInfo specific for SVGZ files, which Play doesn't naturally handle well. */
private[http] case object SVGZContentHeaders
  extends ContentHeaders(MimeTypes.getContentType("a.svg"), Some("gzip"))
