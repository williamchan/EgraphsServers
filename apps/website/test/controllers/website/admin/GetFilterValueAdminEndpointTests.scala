package controllers.website.admin

import org.junit.Test
import play.api.test._
import play.api.test.Helpers._
import services.db.TransactionSerializable
import utils.TestData
import utils._
import utils.FunctionalTestUtils.routeName
import utils.FunctionalTestUtils.Conversions._
import controllers.website._
import controllers.routes.WebsiteControllers.{getFilterValueAdmin, getCreateFilterValueAdmin}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import org.junit.runner.RunWith
import play.api.test.Helpers$

class GetFilterValueAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def db = AppConfig.instance[DBSession]
  override def routeUnderTest = getCreateFilterValueAdmin(1L) 
}

class GetCreateFilterValueAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def db = AppConfig.instance[DBSession]
  override def routeUnderTest = getFilterValueAdmin(1L) 
}