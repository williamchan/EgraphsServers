package controllers.website

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import services.db.TransactionSerializable
import utils.TestData
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.{getCustomerGalleryByUsername, getCustomerGalleryById}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession

class GetCustomerGalleryEndpointTests extends EgraphsUnitTest {

  def db = AppConfig.instance[DBSession]
  
  routeName(getCustomerGalleryById(1L)) should "serve a customer gallery page" in new EgraphsTestApplication {
    val userId = db.connected(TransactionSerializable) {
      TestData.newSavedCustomer().id
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getCustomerGalleryById(userId)))
    status(result) should be (OK)
  }
  
  routeName(getCustomerGalleryByUsername("username")) should "serve a customer gallery page" in new EgraphsTestApplication {
    val username = db.connected(TransactionSerializable) {
      TestData.newSavedCustomer().username
    }
    val Some(result) = routeAndCall(FakeRequest().toRoute(getCustomerGalleryByUsername(username)))
    status(result) should be (OK)
  }
}