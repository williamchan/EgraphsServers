package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postCelebrityFilterValueAdmin
import sjson.json.Serializer
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import models._
import models.filters._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostCelebrityFilterValueAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests {
  override protected def routeUnderTest = postCelebrityFilterValueAdmin(1L)
  override protected def db = AppConfig.instance[DBSession]
  
  it should "reject an invalid celebrity id" in new EgraphsTestApplication {
    val (fv, c) = newFilterValueCeleb
    val result = performRequest(filterValueIds = List(fv.id) , celebrityId = 0L, adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }

 it should "sucessfully create new mappings between celebrities and filter values" in new EgraphsTestApplication {
   val (fv, c) = newFilterValueCeleb
   val result = performRequest(filterValueIds = List(fv.id), celebrityId = c.id, adminId=admin.id)
   status(result) should be (FOUND)
 }
  
  private def performRequest(filterValueIds: Iterable[Long], celebrityId: Long, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postCelebrityFilterValueAdmin(celebrityId)(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
        TestData.toFormUrlSeq("filterValueIds", filterValueIds):_*
      ).withAuthToken
    )
  }    
  
  private def newFilterValueCeleb: (FilterValue, Celebrity)  = {
     db.connected(TransactionSerializable) {
       (TestData.newSavedFilterValue(TestData.newSavedFilter.id), TestData.newSavedCelebrity)
     }
  }
}
