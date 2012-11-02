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
import models.filters._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostFilterValueAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postFilterValueAdmin
  override protected def db = AppConfig.instance[DBSession]
  def filterStore = AppConfig.instance[FilterStore]
  def filterValueStore = AppConfig.instance[FilterValueStore]
  
  routeName(postFilterValueAdmin) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(filterId = newFilterId.toString, filterIds = List(), name = "", publicName=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  routeName(postFilterValueAdmin) should "reject empty publicNames" in new EgraphsTestApplication {
    val result = performRequest(filterId = newFilterId.toString, filterIds = List(), name=TestData.generateUsername(), publicName="", adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  routeName(postFilterValueAdmin) should "sucessfully create new filtervalue for a preexisting filter" in new EgraphsTestApplication {
    val name = TestData.generateUsername()
    val publicName = TestData.generateUsername()
    val filterId = newFilterId
    val result = performRequest(filterId = filterId.toString, filterIds = List(), name=name, publicName=publicName, adminId=admin.id)
    status(result) should be (FOUND)
    
    db.connected(TransactionSerializable) {
      val Some(filter) = filterStore.findById(filterId)
      filter.filterValues.exists(fv => (fv.name == name) && (fv.publicName == publicName)) should be (true)
    }
  }
  
   routeName(postFilterValueAdmin) should "sucessfully associate a filter with a filtervalue" in new EgraphsTestApplication {
    val associatedFilterId = newFilterId
    val newFilterValue = db.connected(TransactionSerializable){ TestData.newSavedFilterValue(newFilterId) }
    val result = performRequest(filterId = newFilterValue.filterId.toString, filterValueId=newFilterValue.id.toString, filterIds = List(associatedFilterId), name=newFilterValue.name, publicName=newFilterValue.publicName, adminId=admin.id)
    
    status(result) should be (FOUND)
    db.connected(TransactionSerializable) { 
      val Some(filterValue) =filterValueStore.findById(newFilterValue.id) 
      filterValue.filters.exists(f => f.id == associatedFilterId) should be (true)
    }
  }
  
  private def performRequest(filterId: String,  filterValueId: String ="0", filterIds: Iterable[Long] = List[Long](), name: String, publicName: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postFilterValueAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
        Seq("filterId" -> filterId,
        "filterValueId" -> filterValueId,    
        "name" -> name,
        "publicName" -> publicName) ++
        TestData.toFormUrlSeq("filterIds", filterIds):_*
      ).withAuthToken
    )
  }
  
  private def newFilterId : Long = {
    db.connected(TransactionSerializable) {TestData.newSavedFilter}.id 
  }
}