package controllers

import play.api.mvc._
import play.api.Play
import play.api.Play.current
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import org.joda.time.DateTimeZone
import scala.Some
import play.api.Logger
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import com.google.common.io.Files

// Inspired by http://www.jamesward.com/2012/08/08/edge-caching-with-play2-heroku-cloudfront

//object RemoteAssets extends Controller {
//
//  private val timeZoneCode = "GMT"
//
//  private val df: DateTimeFormatter =
//    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))
//
//  type ResultWithHeaders = Result { def withHeaders(headers: (String, String)*): Result }
//
//  def at(path: String, file: String): Action[AnyContent] = Action { request =>
//    val action = Assets.at(path, file)
//    val result = action.apply(request)
//    val resultWithHeaders = result.asInstanceOf[ResultWithHeaders]
//    resultWithHeaders.withHeaders(DATE -> df.print({ new java.util.Date }.getTime))
//  }
//
//  def at(file: String): Call = {
//    val secure = Play.configuration.getString("cdn.secure").getOrElse("true").toBoolean // default is secure
//    val httpOrHttps = if (secure) "https://" else "http://"
//
//    val maybeContentUrl = Play.configuration.getString("cdn.contenturl")
//    maybeContentUrl match {
//      case Some(contentUrl) => {
//        new Call("GET", httpOrHttps + contentUrl + controllers.routes.RemoteAssets.at(file).url)
//      }
//      case None => controllers.routes.RemoteAssets.at(file)
//    }
//  }
//}

// trait AssetProvider {
//   def at(file: String): String
//  }
//
//  trait PlayAssets extends AssetProvider {
//    override def at(file: String) = "PlayAssets"
//  }
//
//  trait RemoteAssets extends AssetProvider {
//    abstract override def at(file: String) = super.at(file) + ",RemoteAssets"
//  }
//
//  trait FingerprintedAssets extends AssetProvider {
//    abstract override def at(file: String) = super.at(file) + ", FingerprintedAssets"
//  }
//
//  /* This is the concrete one we use */
//  object EgraphsAssets extends PlayAssets with RemoteAssets with FingerprintedAssets {
//    
//  }
trait AssetProvider { this: Controller =>
  protected def assetReverseRoute(file: String): Call

  def at(path: String, file: String): Action[AnyContent]
  def at(file: String): Call
}

// A concrete implementation using Play's built in Asset controller
trait PlayAssets extends AssetProvider { this: Controller =>
  override def at(path: String, file: String): Action[AnyContent] = controllers.Assets.at(path, file)
  override def at(file: String): Call = assetReverseRoute(file)
}

trait RemoteAssets extends AssetProvider { this: Controller =>
  private val timeZoneCode = "GMT"

  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  type ResultWithHeaders = Result { def withHeaders(headers: (String, String)*): Result }

  abstract override def at(path: String, file: String): Action[AnyContent] = Action { request =>
    val action = super.at(path, file)
    val result = action.apply(request)
    val resultWithHeaders = result.asInstanceOf[ResultWithHeaders]
    resultWithHeaders.withHeaders(DATE -> df.print({ new java.util.Date }.getTime))
  }

  abstract override def at(file: String): Call = {
    val secure = Play.configuration.getString("cdn.secure").getOrElse("true").toBoolean // default is secure
    val httpOrHttps = if (secure) "https://" else "http://"

    val maybeContentUrl = Play.configuration.getString("cdn.contenturl")
    maybeContentUrl match {
      case Some(contentUrl) => {
        new Call("GET", httpOrHttps + contentUrl + this.assetReverseRoute(file).url)
      }
      case None => this.assetReverseRoute(file)
    }
  }
}

trait FingerprintedAssets extends AssetProvider { this: Controller =>
  val useFingerprinting = true // this should be a config look up instead
  val fileToFingerprinted = new ConcurrentHashMap[String, String]()

  abstract override def at(path: String, file: String): Action[AnyContent] = {
    if (!useFingerprinting) {
      super.at(path, file)
    } else {
      println(file + "     ::      " + file.substring(0, file.length - 10))
      super.at(path, file.substring(0, file.length - 10))
      //      super.at(path, use regex to get file)
    }
  }

  abstract override def at(file: String): Call = {
    if (!useFingerprinting) {
      super.at(file)
    } else {
      if (fileToFingerprinted.containsKey(file)) {
        super.at(fileToFingerprinted.get(file))
      } else {
        Play.resource("/public/" + file) match {
          case None => super.at(file)
          case Some(url) =>
            //TODO doing this with adding the "/public/" isn't a great idea, but it works for just now.
            val asset = new File(url.getFile())
            val checksum = Files.getChecksum(asset, new java.util.zip.CRC32)
            val fingerprintedFilename = file.name + "-fp-" +  checksum + file.extension //TODO: this isn't correct, needs to remove and add the extension
            fileToFingerprinted.put(file, fingerprintedFilename)

            super.at(fingerprintedFilename)
        }
      }
    }
  }
}

/* This is the concrete one we use */
object RemoteAssets extends Controller with PlayAssets with RemoteAssets with FingerprintedAssets {
  //TODO: change this to EgraphsAssets
  override def assetReverseRoute(file: String) = controllers.routes.RemoteAssets.at(file)
}