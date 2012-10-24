package controllers.website

import admin.AdminFunctionalTest
import org.junit.Test
import play.test.FunctionalTest
import FunctionalTest._
import services.db.TransactionSerializable
import utils.TestData

class GetCustomerGalleryEndpointTests extends AdminFunctionalTest{

  @Test
  def testRetrievesGalleryOfCustomer() {
    val userId = db.connected(TransactionSerializable) {
      TestData.newSavedCustomer().id
    }
    assertIsOk(GET("/account/" + userId + "/gallery"))
  }
  @Test
  def testRetrievesGalleryOfCustomerByUsername() {
    val username = db.connected(TransactionSerializable) {
      TestData.newSavedCustomer().username
    }
    assertIsOk(GET("/account/" + username))
  }
}