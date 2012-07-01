package controllers.website

import admin.AdminFunctionalTest
import org.junit.Test
import play.test.FunctionalTest
import FunctionalTest._
import services.AppConfig
import services.db.{TransactionSerializable, DBSession}
import utils.{TestConstants, TestData}
import models.enums.PrivacyStatus

class GetCustomerGalleryEndpointTests extends AdminFunctionalTest{
  private val db = AppConfig.instance[DBSession]

  @Test
  def testRetrievesEmptyGalleryOfCustomer() {
    val userId = db.connected(TransactionSerializable) {
      TestData.newSavedCustomer()
    }
    assertIsOk(GET("/account/" + userId + "/gallery"))
  }
}
