package controllers.website.admin

import utils.{TestData, AdminProtectedResourceTests, EgraphsUnitTest}
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import controllers.routes.WebsiteControllers.getCreateMastheadAdmin
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import play.api.test.Helpers._
import scala.Some
import play.api.test.FakeRequest


class GetMastheadAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def routeUnderTest = getCreateMastheadAdmin
  override def db = AppConfig.instance[DBSession]


  routeName(getCreateMastheadAdmin) should "serve a page to create a masthead when logged in" in new EgraphsTestApplication {
    val admin = db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}

    val Some(result) = routeAndCall(FakeRequest().withAdmin(admin.id).toRoute(getCreateMastheadAdmin))
    status(result) should be (OK)
  }

  routeName(getCreateMastheadAdmin) should "not serve a page to create a masthead when not logged in" in new EgraphsTestApplication {
    val Some(result) = routeAndCall(FakeRequest().toRoute(getCreateMastheadAdmin))
    status(result) should be (SEE_OTHER)
  }
}
