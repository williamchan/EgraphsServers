package assetproviders

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.AnyContent
import play.api.mvc.Call

/**
 * This simple interface is meant to mimic the existing interface in Play 2.0
 * for the Assets controller it provides.  By implementing this it is possible
 * to mix and combine various AssetProviders to add additional functionality.
 */
trait AssetProvider { this: Controller =>
  protected def assetReverseRoute(file: String): Call

  /**
   * This is the method that will be called by the router to serve the
   * asset to the client.
   */
  def at(path: String, file: String): Action[AnyContent]

  /**
   * This is the method that will be called by the templates mostly to get
   * a Call that enables them to get the external URL of the asset.
   */
  def at(file: String): Call
}