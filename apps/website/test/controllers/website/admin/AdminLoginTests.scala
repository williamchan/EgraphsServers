package controllers.website.admin

import utils.TestData
import models.Celebrity

import play.api.test.Helpers._
import play.api.test._
import play.api.mvc.Result
import services.db.TransactionSerializable
import utils.{TestConstants, TestData}
import models.enums.{EgraphState, OrderReviewStatus, PrivacyStatus}
import services.AppConfig
import services.db.DBSession
import utils.EgraphsUnitTest
import utils.FunctionalTestUtils.requestWithCustomerId
import services.http.EgraphsSession
import utils.FunctionalTestUtils
import FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers

class AdminLoginTests extends EgraphsUnitTest {

  private def db = AppConfig.instance[DBSession]

  "Admin console" should "should be protected by admin login" in new EgraphsTestApplication {
    val z = db.connected(TransactionSerializable) {
      (TestData.newSavedAdministrator(), TestData.newSavedCelebrity())
    }
    
    val req = FakeRequest().withAdmin(z._1.id)
    val u = WebsiteControllers.getCelebritiesAdmin ().url
    status(routeAndCall(req.copy(method=GET, uri=u)).get) should be(303)

//    assertStatus(302, GET(WebsiteControllers.reverse(WebsiteControllers.getCelebritiesAdmin()).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphsAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityInventoryBatchesAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrdersAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProductsAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(WebsiteControllers.reverse(WebsiteControllers.getCreateCelebrityAdmin).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityInventoryBatchAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProductAdmin", celebrityIdMap).url))
//    assertStatus(302, GET(WebsiteControllers.reverse(WebsiteControllers.getEgraphsAdmin()).url))
//
//    loginAsAdmin()
//    assertStatus(200, GET(WebsiteControllers.reverse(WebsiteControllers.getCelebritiesAdmin()).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphsAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityInventoryBatchesAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrdersAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProductsAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(WebsiteControllers.reverse(WebsiteControllers.getCreateCelebrityAdmin).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityInventoryBatchAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProductAdmin", celebrityIdMap).url))
//    assertStatus(200, GET(WebsiteControllers.reverse(WebsiteControllers.getEgraphsAdmin()).url))
  }
}
