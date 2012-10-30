package controllers.website.admin

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import services.db.TransactionSerializable
import utils.TestData
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.website._
import controllers.routes.WebsiteControllers.{getFiltersAdmin}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import org.junit.runner.RunWith
import play.api.test.Helpers$
import utils.AdminProtectedResourceTests

class GetFiltersAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = getFiltersAdmin() 	
}