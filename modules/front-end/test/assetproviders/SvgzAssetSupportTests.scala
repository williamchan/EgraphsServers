package assetproviders

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import controllers.EgraphsAssets
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.mvc.Controller

@RunWith(classOf[JUnitRunner])
class SvgzAssetSupportTests extends FlatSpec with ShouldMatchers {
  val svgzFileThatExists = "gift-certificate-preview.svgz"

  "SvgzAssetSupport" should "set the correct headers on an svgz file" in {
    running(FakeApplication()) {
      val url = EgraphsAssets.at(svgzFileThatExists).url
      val Some(result) = routeAndCall(FakeRequest(GET, url))

      status(result) should be (OK)
      contentType(result) should be ("image/svg+xml")
      header("Content-Encoding", of=result) should be ("gzip")
    }
  }

  object TestSvgzAssetSupport extends Controller with PlayAssets with SvgzAssetSupport {
    override def assetReverseRoute(file: String) = controllers.routes.EgraphsAssets.at(file)
  }
}