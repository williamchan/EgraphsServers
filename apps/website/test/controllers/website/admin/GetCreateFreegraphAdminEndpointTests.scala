package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests

class GetCreateFreegraphAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.getCreateFreegraphAdmin
}
