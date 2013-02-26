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

  routeUnderTest.url should "associate mastheads as featured" in new EgraphsTestApplication {
    val mastheadId0= newMastheadId
    val mastheadId1 = newMastheadId
    db.connected(TransactionSerializable) {
      val result = controllers.WebsiteControllers.postFeaturedMastheads(FakeRequest().withFormUrlEncodedBody(
      "mastheadIds" -> mastheadId0.toString,
      "mastheadIds" -> mastheadId1.toString
      ).withAuthToken)
      status(result) should be (SEE_OTHER)
      val mastheads = featured.categoryValue.mastheads
      mastheads.size should be (2)
      mastheads.exists(masthead => masthead.id == mastheadId0) should be (true)
      mastheads.exists(masthead => masthead.id == mastheadId1) should be (true)
    }

  }

  private def newMastheadId : Long = {
    db.connected(TransactionSerializable) {
      Masthead(headline = TestData.generateFullname()).save().id
    }
  }
}