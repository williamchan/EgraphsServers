package controllers.website.admin

import controllers.routes.WebsiteControllers.{getCategoryValueAdmin, getCreateCategoryValueAdmin}
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests

class GetFilterValueAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def db = AppConfig.instance[DBSession]
  override def routeUnderTest = getCreateCategoryValueAdmin(1L) 
}

class GetCreateFilterValueAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override def db = AppConfig.instance[DBSession]
  override def routeUnderTest = getCategoryValueAdmin(1L) 
}