package controllers.website.admin

import play.api.test._
import play.api.test.Helpers._
import egraphs.playutils.ResultUtils.RichResult
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.routes.WebsiteControllers.postCategoryAdmin
import utils.EgraphsUnitTest
import utils.CsrfProtectedResourceTests
import utils._
import models.categories._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}

class PostFilterAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests{
  override protected def routeUnderTest = postCategoryAdmin
  override protected def db = AppConfig.instance[DBSession]
  def categoryStore = AppConfig.instance[CategoryStore]

  routeName(routeUnderTest) should "reject empty names" in new EgraphsTestApplication {
    val result = performRequest(categoryId = "0" , name = "", publicName=TestData.generateUsername(), adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  it should "reject empty publicNames" in new EgraphsTestApplication {
    val result = performRequest(categoryId = "0" , name=TestData.generateUsername(), publicName="", adminId=admin.id)
    status(result) should be (SEE_OTHER)
  }
  
  it should "sucessfully create new categories" in new EgraphsTestApplication {
    val categoryName = TestData.generateUsername()
    val categoryPublicName = TestData.generateUsername()
    val result = performRequest(categoryId = "0" , name=categoryName, publicName = categoryPublicName, adminId=admin.id)
    status(result) should be (FOUND)
    db.connected(TransactionSerializable) {
      categoryStore.getCategories.exists(category => (category.name == categoryName) && (category.publicName == categoryPublicName)) should be (true)
    }
  }
  
  private def performRequest(categoryId: String, name: String, publicName: String, adminId: Long): play.api.mvc.Result = {
    controllers.WebsiteControllers.postCategoryAdmin(
      FakeRequest().withAdmin(adminId).withFormUrlEncodedBody(
    	  "categoryId" -> categoryId,
    	  "name" -> name,
    	  "publicName" -> publicName
      ).withAuthToken
    )
  }    
}