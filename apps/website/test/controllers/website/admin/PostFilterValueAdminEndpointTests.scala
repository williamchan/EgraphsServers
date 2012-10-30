package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postFilterValueAdmin
import sjson.json.Serializer
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostFilterValueAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postFilterValueAdmin
  override protected def db = AppConfig.instance[DBSession]

  routeName(postFilterValueAdmin) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(filterId = newFilterId.toString, filterValueId = "0", name = "", publicname=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (BAD_REQUEST)
  }
  
  routeName(postFilterValueAdmin) should "reject empty publicnames" in new EgraphsTestApplication {
    val result = performRequest(filterId = newFilterId.toString, filterValueId = "0", name=TestData.generateUsername(), publicname="", adminId=admin.id)
    status(result) should be (BAD_REQUEST)
  }
  
  routeName(postFilterValueAdmin) should "sucessfully create new filter value for a preexisting filter" in new EgraphsTestApplication {
    val result = performRequest(filterId = newFilterId.toString, filterValueId = "0", name=TestData.generateUsername(), publicname=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (CREATED)
  }
  
  private def performRequest(filterId: String, filterValueId: String, name: String, publicname: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postFilterValueAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
        "filterValueId" -> filterValueId,  
        "filterId" -> filterId,
        "name" -> name,
        "publicName" -> publicname
      ).withAuthToken
    )
  }
  
  private def newFilterId : Long = {
    db.connected(TransactionSerializable) {TestData.newSavedFilter}.id 
  }
}