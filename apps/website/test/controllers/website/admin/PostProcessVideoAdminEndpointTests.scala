package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import utils.AdminProtectedResourceTests
import utils.TestData
import models.VideoAsset
import services.db.{ DBSession, TransactionSerializable, Schema }
import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import models.VideoAssetStore
import models.enums.VideoStatus
import utils.FunctionalTestUtils.Conversions._
import models.VideoAsset

class PostProcessVideoAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.postProcessVideo(VideoStatus.Approved.name, 1L)

  it should "update a video's status" in new EgraphsTestApplication {

    val (videoAsset, admin) = db.connected(TransactionSerializable) {
      val videoAsset = TestData.newSavedVideoAsset()
      val admin = TestData.newSavedAdministrator()
      (videoAsset, admin)
    }

    videoAsset._videoStatus should be(VideoStatus.Unprocessed.name)

    val Some(result) = routeAndCall(FakeRequest(POST, controllers.routes.WebsiteControllers.postProcessVideo(
      VideoStatus.Approved.name, videoAsset.id).url).withAdmin(admin.id).withAuthToken)
    status(result) should be(SEE_OTHER)

    db.connected(TransactionSerializable) {
      val maybeVideoAsset = AppConfig.instance[VideoAssetStore].findById(videoAsset.id)
      maybeVideoAsset.isDefined should be(true)
      maybeVideoAsset.get.videoStatus should be(VideoStatus.Approved)
    }
  }

  
}