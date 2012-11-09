package controllers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.mvc.Action
import play.api.mvc.Call
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import org.specs2.mock.Mockito
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.core.TestApplication
import play.api.test.Helpers._
import play.api.test.TestServer
import play.api.test.FakeApplication
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class EgraphsAssetPipelineTests extends FlatSpec with ShouldMatchers {
  "EgraphAssets" should "use remote assets when available" in {
    running(FakeApplication()) {
      new TestAssets().at("whatever.jpg").url should include ("mycdn.com")
    }
  }

  //
  // Private members
  //
  private class TestAssets extends Controller with EgraphsAssetPipeline {
    override def assetReverseRoute(file: String) = new Call("GET", "/fake-assets/" + file)
    override def defaultPath = "/public"
    override def remoteContentUrl = Some("https://mycdn.com")
    override val cacheControlMaxAgeInSeconds = 10
  }
}