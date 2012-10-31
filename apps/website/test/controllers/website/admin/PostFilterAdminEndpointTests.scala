package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postFilterAdmin
import sjson.json.Serializer
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostFilterAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postFilterAdmin
  override protected def db = AppConfig.instance[DBSession]

  routeName(routeUnderTest) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(filterId = "0" , name = "", publicname=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (BAD_REQUEST)
  }
  
  it should "reject empty publicnames" in new EgraphsTestApplication {
    val result = performRequest(filterId = "0" , name=TestData.generateUsername(), publicname="", adminId=admin.id)
    status(result) should be (BAD_REQUEST)
  }
  
  it should "sucessfully create new filters" in new EgraphsTestApplication {
    val result = performRequest(filterId = "0" , name=TestData.generateUsername(), publicname=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (CREATED)
  }
  
  private def performRequest(filterId: String, name: String, publicname: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postFilterAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
    	  "filterId" -> filterId,
    	  "name" -> name,
    	  "publicName" -> publicname
      ).withAuthToken
    )
  }    
}