package controllers.website.admin

import utils.EgraphsUnitTest
import services.AppConfig
import services.db.DBSession
import utils.AdminProtectedResourceTests

class PostCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.postCouponAdmin(1)
}

class PostCreateCouponAdminEndpointTests extends EgraphsUnitTest with AdminProtectedResourceTests {
  override protected def db = AppConfig.instance[DBSession]
  override protected def routeUnderTest = controllers.routes.WebsiteControllers.postCreateCouponAdmin
}
