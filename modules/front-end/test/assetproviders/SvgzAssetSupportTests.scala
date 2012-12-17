package assetproviders

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.mvc.Controller

@RunWith(classOf[JUnitRunner])
class SvgzAssetSupportTests extends FlatSpec with ShouldMatchers {
  "SvgzAssetSupport" should "set the correct headers on an svgz file" in {
    running(FakeApplication()) {
      val action = TestSvgzAssetSupport.at("/public", "images/gift-certificate-preview.svgz")
      val result = action(FakeRequest())

      status(result) should be (OK)
      contentType(result) should be (Some("image/svg+xml"))
      header("Content-Encoding", of=result) should be (Some("gzip"))
    }
  }

  it should "not screw with other file types" in {
    running(FakeApplication()) {
      val action = TestSvgzAssetSupport.at("/public", "stylesheets/main.min.css")
      val result = action(FakeRequest())

      status(result) should be (OK)
      contentType(result) should not be (Some("image/svg+xml"))
      header("Content-Encoding", of=result) should not be (Some("gzip"))
    }
  }

  private object TestSvgzAssetSupport extends Controller with PlayAssets with SvgzAssetSupport {
    override def assetReverseRoute(file: String) = controllers.routes.EgraphsAssets.at(file)
  }
}