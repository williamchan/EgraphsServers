package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import utils.FunctionalTestUtils._
import controllers.routes.WebsiteControllers.postCategoryValueAdmin
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import models.categories._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostCategoryValueAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postCategoryValueAdmin
  override protected def db = AppConfig.instance[DBSession]
  def categoryStore = AppConfig.instance[CategoryStore]
  def categoryValueStore = AppConfig.instance[CategoryValueStore]
  
  routeName(postCategoryValueAdmin) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(categoryId = newCategoryId.toString, categoryIds = List(), name = "", publicName=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  routeName(postCategoryValueAdmin) should "reject empty publicNames" in new EgraphsTestApplication {
    val result = performRequest(categoryId = newCategoryId.toString, categoryIds = List(), name=TestData.generateUsername(), publicName="", adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  routeName(postCategoryValueAdmin) should "sucessfully create new CategoryValue for a preexisting category" in new EgraphsTestApplication {
    val name = TestData.generateUsername()
    val publicName = TestData.generateUsername()
    val categoryId = newCategoryId
    val result = performRequest(categoryId = categoryId.toString, categoryIds = List(), name=name, publicName=publicName, adminId=admin.id)
    status(result) should be (FOUND)
    
    db.connected(TransactionSerializable) {
      val Some(category) = categoryStore.findById(categoryId)
      category.categoryValues.exists(cv => (cv.name == name) && (cv.publicName == publicName)) should be (true)
    }
  }
  
   routeName(postCategoryValueAdmin) should "sucessfully associate a category with a CategoryValue" in new EgraphsTestApplication {
    val associatedCategoryId = newCategoryId
    val newCategoryValue = db.connected(TransactionSerializable){ TestData.newSavedCategoryValue(newCategoryId) }
    val result = performRequest(categoryId = newCategoryValue.categoryId.toString, categoryValueId=newCategoryValue.id.toString, 
        categoryIds = List(associatedCategoryId), name=newCategoryValue.name, publicName=newCategoryValue.publicName, adminId=admin.id)
    
    status(result) should be (FOUND)
    db.connected(TransactionSerializable) { 
      val Some(categoryValue) = categoryValueStore.findById(newCategoryValue.id) 
      categoryValue.categories.exists(c => c.id == associatedCategoryId) should be (true)
    }
  }
  
  private def performRequest(categoryId: String,  categoryValueId: String ="0", categoryIds: Iterable[Long] = List[Long](), name: String, publicName: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postCategoryValueAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
        Seq("categoryId" -> categoryId,
        "categoryValueId" -> categoryValueId,    
        "name" -> name,
        "publicName" -> publicName) ++
        TestData.toFormUrlSeq("categoryIds", categoryIds):_*
      ).withAuthToken
    )
  }
  
  private def newCategoryId : Long = {
    db.connected(TransactionSerializable) {TestData.newSavedCategory}.id 
  }
}