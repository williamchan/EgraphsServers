package assetproviders

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

/**
 * Fingerprinted assets will change like this:
 *   original      = foo.jpg
 *   fingerprinted = foo-fp-1231343451234.jpg
 */
trait FingerprintedAssets extends AssetProvider { this: Controller =>
  import java.net.URL

  val useFingerprinting = true // this should be a config look up instead
  val fileToFingerprinted = new ConcurrentHashMap[String, String]()

  val fingerprintConstant = "-fp-"

  abstract override def at(path: String, file: String): Action[AnyContent] = {
    if (!useFingerprinting) {
      super.at(path, file)
    } else {
      val (baseFilename, extension) = splitFilename(file)
      if (!baseFilename.contains(fingerprintConstant)) { // this may not have been fingerprinted, we should fall back to without fingerprinting
        super.at(path, file)
      } else {
        val originalBaseFilename = removeAfterLastOccurrence(file, fingerprintConstant)
        super.at(path, originalBaseFilename + "." + extension)
      }
    }
  }

  abstract override def at(file: String): Call = {
    if (!useFingerprinting) {
      super.at(file)
    } else {
      if (fileToFingerprinted.containsKey(file)) {
        super.at(fileToFingerprinted.get(file))
      } else {
        //TODO doing this with adding the "/public/" isn't a great idea, but it works for just now.
        Play.resource("/public/" + file) match {
          case None => super.at(file)
          case Some(url) =>
            val fingerprintedFilename = fingerprintFile(file, url)
            super.at(fingerprintedFilename)
        }
      }
    }
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
