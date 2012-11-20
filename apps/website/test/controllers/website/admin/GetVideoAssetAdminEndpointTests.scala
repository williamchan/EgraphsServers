package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.{ DBSession, TransactionSerializable, Schema }
import utils.AdminProtectedResourceTests
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.{ getVideoAssetAdmin, getVideoAssetsAdmin }
import play.api.test._
import utils.TestData
import play.api.test.Helpers._
import sjson.json.Serializer

class GetVideoAssetAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getVideoAssetAdmin

  routeName(getVideoAssetAdmin()) should "serve a page to upload a video" in new EgraphsTestApplication {
    val admin = db.connected(TransactionSerializable) {
      TestData.newSavedAdministrator()
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getVideoAssetAdmin).withAdmin(admin.id).withAuthToken)
    status(result) should be(OK)
  }
}

class GetVideoAssetsAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getVideoAssetsAdmin

  routeName(getVideoAssetsAdmin()) should "redirect to a page of unprocessed videos when logged in" in new EgraphsTestApplication {
    val admin = db.connected(TransactionSerializable) {
      TestData.newSavedAdministrator()
    }    
    val Some(result) = routeAndCall(FakeRequest().toRoute(getVideoAssetsAdmin).withAdmin(admin.id).withAuthToken)
    status(result) should be(SEE_OTHER)
  }
}
