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
import models.filters._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostFilterAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postFilterAdmin
  override protected def db = AppConfig.instance[DBSession]
  def filterStore = AppConfig.instance[FilterStore]

  routeName(routeUnderTest) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(filterId = "0" , name = "", publicName=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  it should "reject empty publicNames" in new EgraphsTestApplication {
    val result = performRequest(filterId = "0" , name=TestData.generateUsername(), publicName="", adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  it should "sucessfully create new filters" in new EgraphsTestApplication {
    val filtername = TestData.generateUsername()
    val filterpublicName = TestData.generateUsername()
    val result = performRequest(filterId = "0" , name=filtername, publicName = filterpublicName, adminId=admin.id)
    status(result) should be (FOUND)
    db.connected(TransactionSerializable) {
      filterStore.getFilters.exists(filter => (filter.name == filtername) && (filter.publicName == filterpublicName)) should be (true)
    }
  }
  
  private def performRequest(filterId: String, name: String, publicName: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postFilterAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
    	  "filterId" -> filterId,
    	  "name" -> name,
    	  "publicName" -> publicName
      ).withAuthToken
    )
  }    
}