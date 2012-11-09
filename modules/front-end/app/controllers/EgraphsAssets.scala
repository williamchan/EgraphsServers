package controllers

import play.api.Play.current
import play.api.mvc.Controller
import assetproviders.RemoteAssets
import assetproviders.FingerprintedAssets
import assetproviders.PlayAssets

/** Collects all asset transformations into a single trait for use in our website app */
trait EgraphsAssetPipeline
  extends PlayAssets with RemoteAssets with FingerprintedAssets { this: Controller => }

/** The concrete asset implementation for our website app */
object EgraphsAssets extends Controller with EgraphsAssetPipeline {
  override def assetReverseRoute(file: String) = controllers.routes.EgraphsAssets.at(file)

  override def defaultPath = "/public"

  override val remoteContentUrl = {
    val cfg = current.configuration
    
    cfg.getString("cdn.contenturl").map { contentUrl =>
      val secure = cfg.getBoolean("cdn.secure").getOrElse(true)
      val protocol = if (secure) "https" else "http"

      protocol + "://" + contentUrl
    }
  }
}
