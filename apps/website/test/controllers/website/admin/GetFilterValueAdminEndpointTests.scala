package controllers.website.admin

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import services.db.TransactionSerializable
import utils.TestData
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.website._
import controllers.routes.WebsiteControllers.getFilterValueAdmin
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import org.junit.runner.RunWith
import play.api.test.Helpers$

class GetFilterValueAdminEndpointTests extends EgraphsUnitTest {
  private def db = AppConfig.instance[DBSession]
  
  routeName(getFilterValueAdmin(1L)) should "not serve an admin filter value page when not logged in" in new EgraphsTestApplication {
    val filterValueId = db.connected(TransactionSerializable) {
      TestData.newSavedFilterValue(TestData.newSavedFilter.id).id
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getFilterValueAdmin(filterValueId)))
    status(result) should be (SEE_OTHER)
  }
  
  routeName(getFilterValueAdmin(1L)) should "serve an admin filter value page when logged in" in new EgraphsTestApplication {
    val (admin,filterValueId) = db.connected(TransactionSerializable) {
      (TestData.newSavedAdministrator(),       TestData.newSavedFilterValue(TestData.newSavedFilter.id).id)
    }
    
    val Some(result) = routeAndCall(FakeRequest().withAdmin(admin.id).toRoute(getFilterValueAdmin(filterValueId)))
      status(result) should be (OK)
    }
}