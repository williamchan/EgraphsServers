package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.{getUnprocessedVideosAdmin}
import play.api.test._
import play.api.test.Helpers._
import sjson.json.Serializer

class GetVideoEnrollmentAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getVideoEnrollmentAdmin
}

class GetUnprocessedVideosAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getUnprocessedVideosAdmin
  
  routeName(getUnprocessedVideosAdmin()) should "serve a page of unprocessed videos when logged in" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(getUnprocessedVideosAdmin))
    status(result) should be (SEE_OTHER)
  }
}
