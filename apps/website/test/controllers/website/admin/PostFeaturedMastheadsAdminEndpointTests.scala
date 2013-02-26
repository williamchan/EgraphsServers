package controllers.website.admin

import utils.{TestData, AdminProtectedResourceTests, EgraphsUnitTest, CsrfProtectedResourceTests}
import controllers.routes.WebsiteControllers.postFeaturedMastheads
import services.AppConfig
import utils.FunctionalTestUtils._
import services.db.{TransactionSerializable, DBSession}
import models.{MastheadStore, Masthead}
import models.categories.Featured
import play.api.test.FakeRequest
import play.api.test.Helpers._

class PostFeaturedMastheadsAdminEndpointTests extends EgraphsUnitTest with CsrfProtectedResourceTests with AdminProtectedResourceTests {

  override protected def routeUnderTest = postFeaturedMastheads
  override protected def db = AppConfig.instance[DBSession]

  def mastheadStore = AppConfig.instance[MastheadStore]
  def featured = AppConfig.instance[Featured]

  routeUnderTest.url should "associate masthead as featured" in new EgraphsTestApplication {
    val mastheadId = newMastheadId

    db.connected(TransactionSerializable) {
      val result = controllers.WebsiteControllers.postFeaturedMastheads(FakeRequest().withAdmin(admin.id).withFormUrlEncodedBody(
      "mastheadIds" -> mastheadId.toString
      ).withAuthToken)
      status(result) should be (SEE_OTHER)
      val mastheads = featured.categoryValue.mastheads
      mastheads.size should be (1)
      mastheads.exists(masthead => masthead.id == mastheadId) should be (true)
    }
  }

  private def newMastheadId : Long = {
    db.connected(TransactionSerializable) {
      Masthead(headline = TestData.generateFullname()).save().id
    }
  }
}