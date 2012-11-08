package assetproviders

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.AnyContent
import play.api.mvc.Call

//TODO: add comments before checkin
trait AssetProvider { this: Controller =>
  protected def assetReverseRoute(file: String): Call

  def at(path: String, file: String): Action[AnyContent]
  def at(file: String): Call
}