package assetproviders

import assetproviders.ResultWithHeaders.ResultWithHeaders
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.AnyContent

/**
 * Gives asset support for .svgz files, which require the following headers to be processed
 * properly:
 * {{{
 * Content-Type: "image/svg+xml"
 * Content-Encoding: gzip
 * }}}
 */
trait SvgzAssetSupport extends AssetProvider { this: Controller =>
  abstract override def at(path: String, file: String): Action[AnyContent] = {
    if (!file.endsWith(".svgz")) {
      super.at(path, file)
    } else {
      Action { request =>
        val result = super.at(path, file).apply(request).asInstanceOf[ResultWithHeaders]

        result.withHeaders(
          "Content-Encoding" -> "gzip",
          "Content-Type" -> "image/svg+xml"
        )
      }
    }
  }
}
