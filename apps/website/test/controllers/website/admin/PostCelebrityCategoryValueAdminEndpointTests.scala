package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.RichResult._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postCelebrityCategoryValueAdmin
import sjson.json.Serializer
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import models._
import models.categories._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostCelebrityCategoryValueAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests {
  override protected def routeUnderTest = postCelebrityCategoryValueAdmin(1L)
  override protected def db = AppConfig.instance[DBSession]
  
  it should "reject an invalid celebrity id" in new EgraphsTestApplication {
    val (fv, c) = newCategoryValueCeleb
    val result = performRequest(categoryValueIds = List(fv.id) , celebrityId = 0L, adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }

 it should "sucessfully create new mappings between celebrities and category values" in new EgraphsTestApplication {
   val (fv, c) = newCategoryValueCeleb
   val result = performRequest(categoryValueIds = List(fv.id), celebrityId = c.id, adminId=admin.id)
   status(result) should be (FOUND)
   db.connected(TransactionSerializable) {
     c.categoryValues should contain(fv)
   }
 }
  
  private def performRequest(categoryValueIds: Iterable[Long], celebrityId: Long, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postCelebrityCategoryValueAdmin(celebrityId)(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
        TestData.toFormUrlSeq("categoryValueIds", categoryValueIds):_*
      ).withAuthToken
    )
  }    
  
  private def newCategoryValueCeleb: (CategoryValue, Celebrity)  = {
     db.connected(TransactionSerializable) {
       (TestData.newSavedCategoryValue(TestData.newSavedCategory.id), TestData.newSavedCelebrity)
     }
  }
}
