package controllers

import play.api.mvc.Controller
import assetproviders.RemoteAssets
import assetproviders.FingerprintedAssets
import assetproviders.PlayAssets

/* This is the concrete one we use */
object EgraphsAssets extends Controller with PlayAssets with RemoteAssets with FingerprintedAssets {
  override def assetReverseRoute(file: String) = controllers.routes.RemoteAssets.at(file)
}