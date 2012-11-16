package assetproviders

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.AnyContent
import play.api.mvc.Call

/**
 * An AssetProvider view of Play's built in Asset controller that can be mixed
 * into other AssetProviders.
 */
trait PlayAssets extends AssetProvider { this: Controller =>
  override def at(path: String, file: String): Action[AnyContent] = controllers.Assets.at(path, file)
  override def at(file: String): Call = assetReverseRoute(file)
}