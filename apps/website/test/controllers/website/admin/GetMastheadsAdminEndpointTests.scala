package controllers.website.admin

import utils.{AdminProtectedResourceTests, EgraphsUnitTest}
import services.AppConfig
import services.db.DBSession
import controllers.routes.WebsiteControllers.getMastheadsAdmin


class GetMastheadsAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = getMastheadsAdmin()
}
