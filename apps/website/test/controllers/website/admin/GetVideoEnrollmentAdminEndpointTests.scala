package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.{ DBSession, TransactionSerializable, Schema }
import utils.AdminProtectedResourceTests
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.{ getVideoEnrollmentAdmin, getUnprocessedVideosAdmin }
import play.api.test._
import utils.TestData
import play.api.test.Helpers._
import sjson.json.Serializer

class GetVideoEnrollmentAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getVideoEnrollmentAdmin

  routeName(getVideoEnrollmentAdmin()) should "serve a page to upload a video" in new EgraphsTestApplication {
    val admin = db.connected(TransactionSerializable) {
      TestData.newSavedAdministrator()
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getVideoEnrollmentAdmin).withAdmin(admin.id).withAuthToken)
    status(result) should be(OK)
  }
}

class GetUnprocessedVideosAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getUnprocessedVideosAdmin

  routeName(getUnprocessedVideosAdmin()) should "serve a page of unprocessed videos when logged in" in new EgraphsTestApplication {
    val admin = db.connected(TransactionSerializable) {
      TestData.newSavedAdministrator()
    }    
    val Some(result) = routeAndCall(FakeRequest().toRoute(getUnprocessedVideosAdmin).withAdmin(admin.id).withAuthToken)
    status(result) should be(OK)
  }
}
