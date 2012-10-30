package controllers.website.admin

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import services.db.TransactionSerializable
import utils.TestData
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.website._
import controllers.routes.WebsiteControllers.getFilterAdmin
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import org.junit.runner.RunWith
import play.api.test.Helpers$

class GetFilterAdminEndpointTests extends EgraphsUnitTest {
  private def db = AppConfig.instance[DBSession]
  
  routeName(getFilterAdmin(1L)) should "not serve an admin filter page when not logged in" in new EgraphsTestApplication {
    val filterId = db.connected(TransactionSerializable) {
      TestData.newSavedFilter.id
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getFilterAdmin(filterId)))
    status(result) should be (SEE_OTHER)
  }
  
  routeName(getFilterAdmin(1L)) should "serve an admin filter page when logged in" in new EgraphsTestApplication {
    val (admin,filterId) = db.connected(TransactionSerializable) {
      (TestData.newSavedAdministrator(), TestData.newSavedFilter.id)
    }
    
    val Some(result) = routeAndCall(FakeRequest().withAdmin(admin.id).toRoute(getFilterAdmin(filterId)))
      status(result) should be (OK)
    }
}