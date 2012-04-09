package controllers.website.admin

import org.junit.Test
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import play.test.FunctionalTest
import FunctionalTest._
import utils.TestData
import models.Celebrity
import services.{AppConfig, Utils}
import services.db.{DBSession, TransactionSerializable}

class AdminLoginTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  private val db = AppConfig.instance[DBSession]

  @Test
  def testAdminLoginProtectsAdminConsole() {
    createAdmin()
    var celebrity: Celebrity = null
    db.connected(TransactionSerializable) {
      celebrity = TestData.newSavedCelebrity()
    }
    val celebrityIdMap = Map[String, String]("celebrityId" -> celebrity.id.toString)

    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrities").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrity").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getEgraphs").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProducts", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProduct", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphs", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrders", celebrityIdMap).url))

    loginAsAdmin()
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrities").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrity").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getEgraphs").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProducts", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProduct", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphs", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrders", celebrityIdMap).url))
  }

  @Test
  def testAdminLogout() {
    createAndLoginAsAdmin()
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrities").url))
    logoutFromAdminConsole()

    val response = GET(Utils.lookupUrl("WebsiteControllers.getCelebrities").url)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }
}
