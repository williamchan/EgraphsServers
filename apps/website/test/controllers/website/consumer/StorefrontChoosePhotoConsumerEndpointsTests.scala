package controllers.website.consumer

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.status
import play.api.test.FakeRequest
import utils.FunctionalTestUtils._
import utils.DBTransactionPerTest
import utils.EgraphsUnitTest
import utils.TestData
import services.AppConfig
import models.CelebrityStore
import services.db.DBSession
import services.db.TransactionSerializable

@RunWith(classOf[JUnitRunner])
class StorefrontChoosePhotoConsumerEndpointsTests extends EgraphsUnitTest {
   private def db = AppConfig.instance[DBSession]
    
  "postStorefrontChoosePhoto" should "provide a redirect when things work" in new EgraphsTestApplication {
    val (celebrity, product) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      val product = TestData.newSavedProduct(Some(celebrity))
      (celebrity, product)
    }

    val result = controllers.WebsiteControllers.postStorefrontChoosePhoto(celebrity.urlSlug, product.urlSlug)(FakeRequest().withAuthToken)

    status(result) should be(SEE_OTHER)
  }
}