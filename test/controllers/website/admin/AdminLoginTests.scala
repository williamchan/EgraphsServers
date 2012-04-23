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

    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityAdmin").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getEgraphsAdmin").url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProductsAdmin", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProductAdmin", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphsAdmin", celebrityIdMap).url))
    assertStatus(302, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrdersAdmin", celebrityIdMap).url))

    loginAsAdmin()
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityAdmin").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getEgraphsAdmin").url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityProductsAdmin", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCreateCelebrityProductAdmin", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityEgraphsAdmin", celebrityIdMap).url))
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebrityOrdersAdmin", celebrityIdMap).url))
  }

  @Test
  def testAdminLogout() {
    createAndLoginAsAdmin()
    assertStatus(200, GET(Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin").url))
    logoutFromAdminConsole()

    val response = GET(Utils.lookupUrl("WebsiteControllers.getCelebritiesAdmin").url)
    assertStatus(302, response)
    assertHeaderEquals("Location", "/admin/login", response)
  }
}