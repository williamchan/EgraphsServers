package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import utils.TestData
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.{getCategoryAdmin, getCreateCategoryAdmin}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import utils.AdminProtectedResourceTests

class GetCategoryAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def routeUnderTest = getCategoryAdmin(1)
  override def db = AppConfig.instance[DBSession]
  
  routeName(getCreateCategoryAdmin()) should "serve a page to create a category when logged in" in new EgraphsTestApplication {
	val admin = db.connected(TransactionSerializable) {TestData.newSavedAdministrator()}
    
	val Some(result) = route(FakeRequest().withAdmin(admin.id).toCall(getCreateCategoryAdmin))
    status(result) should be (OK)		
  }

  routeName(getCreateCategoryAdmin()) should "not serve a page to create a category page when not logged in" in new EgraphsTestApplication {
	val Some(result) = route(FakeRequest().toCall(getCreateCategoryAdmin))
    status(result) should be (SEE_OTHER)
  }
}