package assetproviders

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
import controllers.EgraphsAssets
import play.api.test.Helpers._
import play.api.test.TestServer
import play.api.test.FakeRequest

@RunWith(classOf[JUnitRunner])
class RemoteAssetTests extends FlatSpec with ShouldMatchers {

  "RemoteAssets" should "not create CDN links when no content URL was provided" in {
    remoteAssetsDisabled.at("whatever").url should be ("/fake-assets/whatever")
  }

  it should "create cdn links when content URL is provided" in {
    remoteAssetsEnabled.at("whatever").url should be ("https://mycdn.cloudfront.net/fake-assets/whatever")
  }

  //
  // Private members
  //
  private case class TestRemoteAssets(remoteContentUrl: Option[String]) extends Controller with PlayAssets with RemoteAssets {
    override def assetReverseRoute(file: String) = new Call("GET", "/fake-assets/" + file)
  }

  private val remoteAssetsDisabled = {
    TestRemoteAssets(None)
  }

  private val remoteAssetsEnabled = {
    remoteAssetsDisabled.copy(Some("https://mycdn.cloudfront.net"))
  }
}