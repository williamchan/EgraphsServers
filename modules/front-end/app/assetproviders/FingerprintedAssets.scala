package assetproviders

import assetproviders.ResultWithHeaders.ResultWithHeaders
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import com.google.common.io.Files
import play.api.Play.current
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.Play
import play.api.Logger

/**
 * Pipelines fingerprinting for your static assets, which allows you to improve site
 * performance by setting very long cache expiries. Assets are fingerprinted like this:
 *   original      = foo.jpg
 *   fingerprinted = foo-fp-1231343451234.jpg
 *
 * Where '-fp-1231343451234' is the 'fingerprint' and '1231343451234' is a checksum of
 * the file contents.
 *
 * We got some inspiration from ruby on rails, but the solution was a bit obvious.
 * http://guides.rubyonrails.org/asset_pipeline.html#what-is-fingerprinting-and-why-should-i-care
 */
trait FingerprintedAssets extends AssetProvider { this: Controller =>
  import java.net.URL

  private val fileToFingerprinted = new ConcurrentHashMap[String, String]()

  private val fingerprintConstant = "-fp-"

  def defaultPath: String

  /**
   * This will find files that were fingerprinted.  If the file is found with the fingerprint
   * striped out of its name and the checksum matches the checksum in the fingerprint.  Otherwise
   * it will just return whatever the super.at(path, file) returns.
   */
  abstract override def at(path: String, file: String): Action[AnyContent] = {
    val (baseFilename, extension) = splitFilename(file)
    if (!baseFilename.contains(fingerprintConstant)) { // this may not have been fingerprinted, we should fall back to without fingerprinting
      super.at(path, file)
    } else {
      val (originalBaseFilename, fingerprint) = file.splitAt(file.lastIndexOf(fingerprintConstant))
      val originalFilename = originalBaseFilename + "." + extension

      val originalOrFingerprinted = originalOrFingerprint(originalFilename)

      originalOrFingerprinted.fold(
        originalFilename => {
          Logger.info("Could not find asset at " + originalFilename + " for file =" + file + " in path = " + path)
          super.at(path, file) // maybe another asset provider knows what to do with this 
        },
        fingerprintedFilename => {
          if (fingerprintedFilename != file) {
            Logger.info("expected checksum = " + file + " but we a file with a different checksum = " + fingerprintedFilename)
            super.at(path, file) // again, this asset provider doesn't have this asset
          } else {
            Action { request =>
              val action = super.at(path, originalFilename)
              val result = action.apply(request)
              val resultWithHeaders = result.asInstanceOf[ResultWithHeaders]
              resultWithHeaders.withHeaders(CACHE_CONTROL -> "max-age=31536000")
            }
          }
        })
    }
  }

  abstract override def at(file: String): Call = {
    val originalOrFingerprinted = originalOrFingerprint(file)

    originalOrFingerprinted.fold({
      originalFilename => super.at(originalFilename)
    }, {
      fingerprintedFilename => super.at(fingerprintedFilename)
    })
  }

  /**
   * If there the original file exists and can be fingerprinted or has been fingerprinted the
   * fingerprinted filename is on the Right, otherwise the originalFilename is on the Left.
   */
  private def originalOrFingerprint(originalFilename: String): Either[String, String] = {
    if (fileToFingerprinted.contains(originalFilename)) {
      Right(fileToFingerprinted.get(originalFilename))
    } else {
      defaultUrl(originalFilename) match {
        case None =>
          Left(originalFilename)
        case Some(url) =>
          val fingerprintedFilename = fingerprintFile(originalFilename, url)
          Right(fingerprintedFilename)
      }
    }
  }

  private def defaultUrl(file: String) = {
    Play.resource(defaultPath + "/" + file)
  }

  // Updates the fileToFingerprinted map with the fingerprinted file
  private def fingerprintFile(file: String, url: URL): String = {
    val checksum = getChecksum(url)
    val (baseFilename, extension) = splitFilename(file)
    val fingerprintedFilename = baseFilename + fingerprintConstant + checksum + "." + extension

    fileToFingerprinted.put(file, fingerprintedFilename)
    fingerprintedFilename
  }

  private def getChecksum(url: URL): Long = {
    val asset = new File(url.getFile())
    Files.getChecksum(asset, new java.util.zip.CRC32)
  }

  private def splitFilename(filename: String): (String, String) = {
    val extension = getFileExtension(filename)
    val uncleanBaseFilename = removeAfterLastOccurrence(filename, extension)
    val baseFilename = if (uncleanBaseFilename.endsWith("."))
      uncleanBaseFilename.dropRight(1)
    else uncleanBaseFilename

    (baseFilename, extension)
  }

  // returns everything after last occurrence of the other string, requires
  //first arg must contain second arg
  private def removeAfterLastOccurrence(str: String, toRemove: String) = {
    str.take(str.lastIndexOf(toRemove))
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file
   * extension</a> for the given file name, or the empty string if the file has
   * no extension.  The result does not include the '{@code .}'.
   *
   * Adopted from Google Guava r11 since Play 2.0 only gives us Guava r10
   */
  private def getFileExtension(fullName: String): String = {
    val fileName = new File(fullName).getName()
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex == -1)
      ""
    else fileName.substring(dotIndex + 1)
  }
}
