package controllers

import play.api.Play.current
import play.api.mvc.Controller
import assetproviders.{SvgzAssetSupport, RemoteAssets, FingerprintedAssets, PlayAssets}

/** Collects all asset transformations into a single trait for use in our website app */
trait EgraphsAssetPipeline
  extends PlayAssets
     with RemoteAssets
//     with FingerprintedAssets
     with SvgzAssetSupport { this: Controller => }

/** The concrete asset implementation for our website app */
object EgraphsAssets extends Controller with EgraphsAssetPipeline {
  override def assetReverseRoute(file: String) = controllers.routes.EgraphsAssets.at(file)

   val defaultPath = "/public"

  override val remoteContentUrl = {
    val cfg = current.configuration

    cfg.getString("cdn.contenturl").map { contentUrl =>
      val secure = cfg.getBoolean("cdn.secure").getOrElse(true)
      val protocol = if (secure) "https" else "http"

      protocol + "://" + contentUrl
    }
  }

   val cacheControlMaxAgeInSeconds = {
    val cfg = current.configuration

    cfg.getInt("assets.immutable.cacheControlInSeconds").getOrElse(31536000) // default 1 metric year
  }
}
