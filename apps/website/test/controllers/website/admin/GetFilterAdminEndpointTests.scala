package controllers.website.admin

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import utils.TestData
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.website._
import controllers.routes.WebsiteControllers.{getFilterAdmin, getCreateFilterAdmin}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import org.junit.runner.RunWith
import play.api.test.Helpers$
import utils.AdminProtectedResourceTests

class GetFilterAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def routeUnderTest = getFilterAdmin(1)
  override def db = AppConfig.instance[DBSession]
  
  routeName(getCreateFilterAdmin()) should "serve a page to create a filter page when logged in" in new EgraphsTestApplication {
	  val admin = db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
    
	  val Some(result) = routeAndCall(FakeRequest().withAdmin(admin.id).toRoute(getCreateFilterAdmin))
    status(result) should be (OK)		
  }
  
  routeName(getCreateFilterAdmin()) should "not serve a page to create a filter page when not logged in" in new EgraphsTestApplication {
	val Some(result) = routeAndCall(FakeRequest().toRoute(getCreateFilterAdmin))
    status(result) should be (SEE_OTHER)		
  }
}