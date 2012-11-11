package controllers.website.admin

import controllers.routes.WebsiteControllers.getCategoriesAdmin
import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests

class GetFiltersAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = getCategoriesAdmin() 	
}