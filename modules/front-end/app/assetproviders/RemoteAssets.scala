package assetproviders

import assetproviders.ResultWithHeaders.ResultWithHeaders

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.DateTimeZone

import play.api.Play.current
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.Play

// Inspired by http://www.jamesward.com/2012/08/08/edge-caching-with-play2-heroku-cloudfront
trait RemoteAssets extends AssetProvider { this: Controller =>
  private val timeZoneCode = "GMT"

  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

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


